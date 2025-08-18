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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    
    public void sendStreamingMessage(SendMessageRequest request) {
        log.info("Processing streaming message for chat room: {}", request.getChatRoomId());
        
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
            
            // 3. LLM 요청을 Kafka로 발행
            String requestId = UUID.randomUUID().toString();
            LlmRequest llmRequest = LlmRequest.builder()
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .userMessage(request.getMessage())
                    .requestId(requestId)
                    .build();
            
            // Kafka로 LLM 요청 발행
            messagePublisher.publishLlmRequest(llmRequest);
            log.info("Published LLM request to Kafka: {}", requestId);
            
        } catch (Exception e) {
            log.error("Error processing streaming message for chat room: {}", request.getChatRoomId(), e);
            throw new RuntimeException("채팅방 처리 중 오류가 발생했습니다.", e);
        }
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

    /**
     * Kafka로부터 LLM 응답을 구독하여 DB에 저장
     */
    @KafkaListener(topics = "llm-response", groupId = "chat-service")
    public void handleLlmResponse(Map<String, Object> responseData) {
        try {
            log.info("Received LLM response from Kafka: {}", responseData.get("requestId"));
            
            String chatRoomId = (String) responseData.get("chatRoomId");
            String userId = (String) responseData.get("userId");
            String message = (String) responseData.get("message");
            String requestId = (String) responseData.get("requestId");
            
            // 채팅방 조회
            ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));
            
            // LLM 응답을 채팅방에 추가
            ChatMessage llmMessage = new ChatMessage(
                chatRoomId,
                "assistant", // LLM 응답은 assistant로 표시
                message
            );
            
            chatRoom.addMessage(llmMessage);
            chatRepository.save(chatRoom);
            
            // Kafka 이벤트 발행 (LLM 응답 완료)
            messagePublisher.publishChatEvent(
                chatRoomId, 
                "LLM_RESPONSE_COMPLETED", 
                userId
            );
            
            log.info("Successfully saved LLM response to chat room: {}, requestId: {}", chatRoomId, requestId);
            
        } catch (Exception e) {
            log.error("Error handling LLM response from Kafka", e);
        }
    }
    
    /**
     * Kafka로부터 LLM 오류를 구독하여 처리
     */
    @KafkaListener(topics = "llm-error", groupId = "chat-service")
    public void handleLlmError(Map<String, Object> errorData) {
        try {
            log.info("Received LLM error from Kafka: {}", errorData.get("requestId"));
            
            String chatRoomId = (String) errorData.get("chatRoomId");
            String userId = (String) errorData.get("userId");
            String error = (String) errorData.get("error");
            String requestId = (String) errorData.get("requestId");
            
            // 오류 메시지를 채팅방에 추가
            ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));
            
            ChatMessage errorMessage = new ChatMessage(
                chatRoomId,
                "assistant",
                "죄송합니다. 응답 생성 중 오류가 발생했습니다: " + error
            );
            
            chatRoom.addMessage(errorMessage);
            chatRepository.save(chatRoom);
            
            log.info("Saved LLM error message to chat room: {}, requestId: {}", chatRoomId, requestId);
            
        } catch (Exception e) {
            log.error("Error handling LLM error from Kafka", e);
        }
    }
} 