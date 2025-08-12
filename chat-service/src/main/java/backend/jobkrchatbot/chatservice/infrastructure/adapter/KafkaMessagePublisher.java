package backend.jobkrchatbot.chatservice.infrastructure.adapter;

import backend.jobkrchatbot.chatservice.domain.model.ChatRoomId;
import backend.jobkrchatbot.chatservice.domain.model.MessageId;
import backend.jobkrchatbot.chatservice.domain.port.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessagePublisher implements MessagePublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Override
    public void publishUserMessage(ChatRoomId chatRoomId, MessageId messageId, String userMessage) {
        try {
            // LLM 요청을 Kafka로 전송
            LlmRequest request = LlmRequest.builder()
                    .requestId(messageId.toString())
                    .chatRoomId(chatRoomId.toString())
                    .userMessage(userMessage)
                    .build();
            
            kafkaTemplate.send("llm.requests", messageId.toString(), request);
            log.info("User message published to Kafka: {}", messageId.toString());
            
        } catch (Exception e) {
            log.error("Failed to publish user message to Kafka", e);
            throw new RuntimeException("메시지 발행 실패", e);
        }
    }
    
    @Override
    public void publishSystemMessage(ChatRoomId chatRoomId, String systemMessage) {
        try {
            // 시스템 메시지를 Kafka로 전송
            SystemMessage message = SystemMessage.builder()
                    .chatRoomId(chatRoomId.toString())
                    .content(systemMessage)
                    .build();
            
            kafkaTemplate.send("system.messages", chatRoomId.toString(), message);
            log.info("System message published to Kafka for chat room: {}", chatRoomId.toString());
            
        } catch (Exception e) {
            log.error("Failed to publish system message to Kafka", e);
            throw new RuntimeException("시스템 메시지 발행 실패", e);
        }
    }
    
    @Override
    public void publishChatEvent(ChatRoomId chatRoomId, String eventType, Object eventData) {
        try {
            // 채팅 이벤트를 Kafka로 전송
            ChatEvent event = ChatEvent.builder()
                    .chatRoomId(chatRoomId.toString())
                    .eventType(eventType)
                    .eventData(eventData)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            kafkaTemplate.send("chat.events", chatRoomId.toString(), event);
            log.info("Chat event published to Kafka: {} - {}", eventType, chatRoomId.toString());
            
        } catch (Exception e) {
            log.error("Failed to publish chat event to Kafka", e);
            throw new RuntimeException("채팅 이벤트 발행 실패", e);
        }
    }
    
    // 내부 DTO 클래스들
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LlmRequest {
        private String requestId;
        private String chatRoomId;
        private String userMessage;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SystemMessage {
        private String chatRoomId;
        private String content;
    }
    
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatEvent {
        private String chatRoomId;
        private String eventType;
        private Object eventData;
        private long timestamp;
    }
} 