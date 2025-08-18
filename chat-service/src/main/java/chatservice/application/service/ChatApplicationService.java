package chatservice.application.service;

import chatservice.domain.model.ChatMessage;
import chatservice.domain.model.ChatRoom;
import chatservice.domain.port.ChatRepository;
import chatservice.domain.port.MessagePublisher;
import chatservice.domain.port.LlmService;
import chatservice.infrastructure.client.LlmClient;
import chatservice.infrastructure.client.dto.LlmRequest;
import chatservice.application.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatApplicationService {
    
    private final ChatRepository chatRepository;
    private final MessagePublisher messagePublisher;
    private final LlmService llmService;
    private final LlmClient llmClient;
    
    public ChatRoomResponse startChat(StartChatRequest request) {
        log.info("Starting chat for user: {}", request.getUserId());
        
        // 채팅방 생성
        ChatRoom chatRoom = new ChatRoom(request.getUserId());
        
        // 채팅방 저장
        ChatRoom savedChatRoom = chatRepository.save(chatRoom);
        
        // 채팅 시작 이벤트 발행
        messagePublisher.publishChatEvent(savedChatRoom.getId(), "CHAT_STARTED", 
                request.getUserId());
        
        return ChatRoomResponse.from(savedChatRoom);
    }
    
    public ChatResponse sendMessage(SendMessageRequest request) {
        log.info("Processing message for chat room: {}", request.getChatRoomId());
        
        ChatRoom chatRoom = chatRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + request.getChatRoomId()));
        
        // 사용자 메시지 생성 및 추가
        ChatMessage userMessage = new ChatMessage(
            request.getChatRoomId(), 
            request.getUserId(), 
            request.getMessage()
        );
        
        chatRoom.addMessage(userMessage);
        // 메시지가 추가된 채팅방을 저장
        chatRepository.save(chatRoom);
        
        // LLM 서비스로 HTTP 동기 요청 전송
        String requestId = UUID.randomUUID().toString();
        
        try {
            String llmResponse = llmService.generateResponse(
                request.getChatRoomId(), 
                request.getUserId(), 
                request.getMessage(), 
                requestId
            );
            
            // LLM 응답을 채팅방에 추가
            ChatMessage llmMessage = new ChatMessage(
                request.getChatRoomId(),
                "assistant", // LLM 응답은 assistant로 표시
                llmResponse
            );
            
            chatRoom.addMessage(llmMessage);
            chatRepository.save(chatRoom);
            
            // 실제 LLM 응답 반환
            return ChatResponse.builder()
                    .message(llmResponse)
                    .chatRoomId(request.getChatRoomId())
                    .requestId(requestId)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting LLM response for chat room: {}", request.getChatRoomId(), e);
            
            // 오류 발생 시 기본 응답
            String errorMessage = "죄송합니다. 현재 서비스에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";
            
            ChatMessage errorChatMessage = new ChatMessage(
                request.getChatRoomId(),
                "assistant",
                errorMessage
            );
            
            chatRoom.addMessage(errorChatMessage);
            chatRepository.save(chatRoom);
            
            return ChatResponse.builder()
                    .message(errorMessage)
                    .chatRoomId(request.getChatRoomId())
                    .requestId(requestId)
                    .build();
        }
    }
    
    public SseEmitter sendStreamingMessage(SendMessageRequest request) {
        log.info("Processing streaming message for chat room: {}", request.getChatRoomId());
        
        SseEmitter emitter = new SseEmitter(60000L); // 60초 타임아웃
        
        try {
            // 1. 채팅방 검증 및 사용자 메시지 저장
            ChatRoom chatRoom = chatRepository.findById(request.getChatRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + request.getChatRoomId()));
            
            // 사용자 메시지 생성 및 추가
            ChatMessage userMessage = new ChatMessage(
                request.getChatRoomId(), 
                request.getUserId(), 
                request.getMessage()
            );
            
            chatRoom.addMessage(userMessage);
            chatRepository.save(chatRoom);
            
            // 2. Kafka 이벤트 발행 (사용자 메시지)
            messagePublisher.publishChatEvent(
                request.getChatRoomId(), 
                "USER_MESSAGE_SENT", 
                request.getUserId()
            );
            
            // 3. LLM 스트리밍 요청 생성
            String requestId = UUID.randomUUID().toString();
            LlmRequest llmRequest = LlmRequest.builder()
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .userMessage(request.getMessage())
                    .requestId(requestId)
                    .build();
            
            // 4. 실시간 SSE 프록시 처리
            handleRealTimeStreaming(llmRequest, emitter, chatRoom);
            
        } catch (Exception e) {
            log.error("Error initializing streaming message for chat room: {}", request.getChatRoomId(), e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"채팅방 처리 중 오류가 발생했습니다.\"}"));
                emitter.completeWithError(e);
            } catch (IOException ioException) {
                log.error("Error sending error event", ioException);
            }
        }
        
        return emitter;
    }
    
    @Async
    public CompletableFuture<Void> handleRealTimeStreaming(LlmRequest llmRequest, SseEmitter emitter, ChatRoom chatRoom) {
        StringBuilder responseBuilder = new StringBuilder();
        
        try {
            // 스트리밍 시작 이벤트를 프론트엔드로 전송
            emitter.send(SseEmitter.event()
                    .name("streaming_started")
                    .data("{\"requestId\":\"" + llmRequest.getRequestId() + "\",\"chatRoomId\":\"" + llmRequest.getChatRoomId() + "\"}"));
            
            // LLM 서비스로부터 SSE 스트림을 받아서 실시간으로 프론트엔드에 전달
            llmClient.generateStreamingResponse(llmRequest)
                    .doOnNext(event -> {
                        try {
                            String eventName = event.event();
                            String data = event.data();
                            
                            // LLM 서비스의 이벤트를 그대로 프론트엔드로 전달
                            emitter.send(SseEmitter.event()
                                    .name(eventName)
                                    .data(data));
                            
                            // chunk 데이터 누적 (최종 저장용)
                            if ("chunk".equals(eventName)) {
                                responseBuilder.append(data).append(" ");
                            }
                            
                            // Kafka 로깅 (실시간 스트리밍 진행 상황)
                            if ("chunk".equals(eventName)) {
                                messagePublisher.publishChatEvent(
                                    llmRequest.getChatRoomId(), 
                                    "LLM_CHUNK_RECEIVED", 
                                    llmRequest.getUserId()
                                );
                            }
                            
                        } catch (IOException e) {
                            log.error("Error proxying streaming event to frontend", e);
                            emitter.completeWithError(e);
                        }
                    })
                    .doOnError(error -> {
                        log.error("Error in LLM streaming response", error);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("{\"error\":\"LLM 서비스 오류가 발생했습니다.\"}"));
                            
                            // Kafka 오류 로깅
                            messagePublisher.publishChatEvent(
                                llmRequest.getChatRoomId(), 
                                "LLM_STREAMING_ERROR", 
                                llmRequest.getUserId()
                            );
                            
                            emitter.completeWithError(error);
                        } catch (IOException e) {
                            log.error("Error sending error event to frontend", e);
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            // 스트리밍 완료 처리
                            String fullResponse = responseBuilder.toString().trim();
                            
                            if (!fullResponse.isEmpty()) {
                                // LLM 응답을 채팅방에 저장
                                ChatMessage llmMessage = new ChatMessage(
                                    llmRequest.getChatRoomId(),
                                    "assistant",
                                    fullResponse
                                );
                                
                                chatRoom.addMessage(llmMessage);
                                chatRepository.save(chatRoom);
                                
                                // Kafka 이벤트 발행 (LLM 응답 완료)
                                messagePublisher.publishChatEvent(
                                    llmRequest.getChatRoomId(), 
                                    "LLM_RESPONSE_COMPLETED", 
                                    llmRequest.getUserId()
                                );
                            }
                            
                            // 완료 이벤트를 프론트엔드로 전송
                            emitter.send(SseEmitter.event()
                                    .name("streaming_completed")
                                    .data("{\"requestId\":\"" + llmRequest.getRequestId() + "\",\"fullResponse\":\"" + fullResponse.replace("\"", "\\\"") + "\"}"));
                            
                            emitter.complete();
                            log.info("Streaming completed and saved for request: {}", llmRequest.getRequestId());
                            
                        } catch (Exception e) {
                            log.error("Error completing streaming", e);
                            emitter.completeWithError(e);
                        }
                    })
                    .subscribe(); // 구독 시작
            
        } catch (Exception e) {
            log.error("Error setting up streaming proxy", e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\":\"스트리밍 설정 중 오류가 발생했습니다.\"}"));
                emitter.completeWithError(e);
            } catch (IOException ioException) {
                log.error("Error sending error event", ioException);
            }
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    public void saveMessage(SaveMessageRequest request) {
        log.info("Saving message for chat room: {}", request.getChatRoomId());
        
        try {
            ChatRoom chatRoom = chatRepository.findById(request.getChatRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + request.getChatRoomId()));
            
            ChatMessage message = new ChatMessage(
                request.getChatRoomId(),
                request.getUserId(),
                request.getMessage()
            );
            
            chatRoom.addMessage(message);
            chatRepository.save(chatRoom);
            
            // Kafka 이벤트 발행
            messagePublisher.publishChatEvent(
                request.getChatRoomId(), 
                "MESSAGE_SAVED", 
                request.getUserId()
            );
            
        } catch (Exception e) {
            log.error("Error saving message for chat room: {}", request.getChatRoomId(), e);
            throw new RuntimeException("메시지 저장 중 오류가 발생했습니다.", e);
        }
    }
    
    public ChatHistoryResponse getChatHistory(String chatRoomId) {
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));
        
        List<ChatMessage> messages = chatRoom.getRecentMessages(50); // 최근 50개 메시지
        
        return ChatHistoryResponse.builder()
                .chatRoomId(chatRoomId)
                .messages(messages.stream()
                        .map(ChatMessageResponse::from)
                        .toList())
                .build();
    }
    
    public List<ChatRoomSummaryResponse> getUserChatRooms(String userId) {
        List<ChatRoom> chatRooms = chatRepository.findByUserId(userId);
        
        return chatRooms.stream()
                .map(ChatRoomSummaryResponse::from)
                .toList();
    }
} 