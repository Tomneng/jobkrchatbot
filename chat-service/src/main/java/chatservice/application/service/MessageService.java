package chatservice.application.service;

import chatservice.domain.model.ChatMessage;
import chatservice.domain.model.ChatRoom;
import chatservice.domain.port.ChatRepository;
import chatservice.domain.port.MessagePublisher;
import chatservice.application.dto.SaveMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {
    
    private final ChatRepository chatRepository;
    private final MessagePublisher messagePublisher;
    
    /**
     * 사용자 메시지를 채팅방에 저장
     */
    public ChatRoom saveUserMessage(String chatRoomId, String userId, String message) {
        ChatRoom chatRoom = findChatRoom(chatRoomId);
        
        ChatMessage userMessage = new ChatMessage(chatRoomId, userId, message);
        chatRoom.addMessage(userMessage);
        
        return chatRepository.save(chatRoom);
    }
    
    /**
     * LLM 응답을 채팅방에 저장
     */
    public void saveLlmMessage(String chatRoomId, String message) {
        ChatRoom chatRoom = findChatRoom(chatRoomId);
        
        ChatMessage llmMessage = new ChatMessage(chatRoomId, "assistant", message);
        chatRoom.addMessage(llmMessage);
        
        chatRepository.save(chatRoom);
    }

    /**
     * Kafka로부터 받은 LLM 응답 저장
     */
    public void saveLlmResponseFromKafka(Map<String, Object> responseData) {
        try {
            String chatRoomId = (String) responseData.get("chatRoomId");
            String userId = (String) responseData.get("userId");
            String message = (String) responseData.get("message");
            String requestId = (String) responseData.get("requestId");
            
            log.info("Saving LLM response from Kafka: {}", requestId);
            
            saveLlmMessage(chatRoomId, message);
            
            // LLM 응답 완료 이벤트 발행
            messagePublisher.publishChatEvent(chatRoomId, "LLM_RESPONSE_COMPLETED", userId);
            
            log.info("Successfully saved LLM response to chat room: {}, requestId: {}", chatRoomId, requestId);
            
        } catch (Exception e) {
            log.error("Error handling LLM response from Kafka", e);
        }
    }
    
    /**
     * Kafka로부터 받은 LLM 오류 처리
     */
    public void saveLlmErrorFromKafka(Map<String, Object> errorData) {
        try {
            String chatRoomId = (String) errorData.get("chatRoomId");
            String error = (String) errorData.get("error");
            String requestId = (String) errorData.get("requestId");
            
            log.info("Saving LLM error from Kafka: {}", requestId);
            
            String errorMessage = "죄송합니다. 응답 생성 중 오류가 발생했습니다: " + error;
            saveLlmMessage(chatRoomId, errorMessage);
            
            log.info("Saved LLM error message to chat room: {}, requestId: {}", chatRoomId, requestId);
            
        } catch (Exception e) {
            log.error("Error handling LLM error from Kafka", e);
        }
    }
    
    private ChatRoom findChatRoom(String chatRoomId) {
        return chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));
    }
} 