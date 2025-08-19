package chatservice.application.service;

import chatservice.domain.model.ChatMessage;
import chatservice.domain.model.ChatRoom;
import chatservice.domain.port.ChatRepository;
import chatservice.domain.port.MessagePublisher;
import chatservice.application.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatApplicationService {
    
    private final ChatRepository chatRepository;
    private final MessagePublisher messagePublisher;
    private final MessageService messageService;
    private final LlmIntegrationService llmIntegrationService;
    
    /**
     * 새로운 채팅방 시작
     */
    public ChatRoomResponse startChat(StartChatRequest request) {
        log.info("Starting chat for user: {}", request.getUserId());
        
        ChatRoom chatRoom = new ChatRoom(request.getUserId());
        ChatRoom savedChatRoom = chatRepository.save(chatRoom);
        
        // 채팅 시작 이벤트 발행
        messagePublisher.publishChatEvent(
            savedChatRoom.getId(), 
            "CHAT_STARTED", 
            request.getUserId()
        );
        
        return ChatRoomResponse.from(savedChatRoom);
    }
    
    /**
     * 비동기 스트리밍 메시지 전송
     */
    public void sendStreamingMessage(SendMessageRequest request) {
        log.info("Processing streaming message for chat room: {}", request.getChatRoomId());
        
        // 1. 사용자 메시지 저장
        messageService.saveUserMessage(
            request.getChatRoomId(), 
            request.getUserId(), 
            request.getMessage()
        );
        
        // 2. 스트리밍 LLM 요청 발행
        llmIntegrationService.requestStreamingResponse(request);
    }
    
    /**
     * 메시지 저장 (외부 요청)
     */
    public void saveMessage(SaveMessageRequest request) {
        messageService.saveMessage(request);
    }
    
    /**
     * 채팅 히스토리 조회
     */
    public ChatHistoryResponse getChatHistory(String chatRoomId) {
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));
        
        List<ChatMessage> messages = chatRoom.getRecentMessages(50);
        
        return ChatHistoryResponse.builder()
                .chatRoomId(chatRoomId)
                .messages(messages.stream()
                        .map(ChatMessageResponse::from)
                        .toList())
                .build();
    }
    
    /**
     * 사용자별 채팅방 목록 조회
     */
    public List<ChatRoomSummaryResponse> getUserChatRooms(String userId) {
        List<ChatRoom> chatRooms = chatRepository.findByUserId(userId);
        
        return chatRooms.stream()
                .map(ChatRoomSummaryResponse::from)
                .toList();
    }

    /**
     * Kafka로부터 LLM 응답 수신 처리
     */
    @KafkaListener(topics = "llm-response", groupId = "chat-service")
    public void handleLlmResponse(Map<String, Object> responseData) {
        messageService.saveLlmResponseFromKafka(responseData);
    }
    
    /**
     * Kafka로부터 LLM 오류 수신 처리
     */
    @KafkaListener(topics = "llm-error", groupId = "chat-service")
    public void handleLlmError(Map<String, Object> errorData) {
        messageService.saveLlmErrorFromKafka(errorData);
    }
} 