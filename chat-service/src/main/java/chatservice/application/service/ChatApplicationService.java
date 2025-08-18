package chatservice.application.service;

import chatservice.domain.model.ChatMessage;
import chatservice.domain.model.ChatRoom;
import chatservice.domain.port.ChatRepository;
import chatservice.domain.port.MessagePublisher;
import chatservice.domain.port.LlmService;
import chatservice.application.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatApplicationService {
    
    private final ChatRepository chatRepository;
    private final MessagePublisher messagePublisher;
    private final LlmService llmService;
    
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