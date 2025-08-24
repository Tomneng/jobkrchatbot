package chatservice.infrastructure.adapter;

import chatservice.domain.port.MessagePublisher;
import chatservice.infrastructure.client.dto.LlmRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessagePublisher implements MessagePublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CHAT_EVENTS_TOPIC = "chat-events";
    private static final String USER_MESSAGES_TOPIC = "user-messages";
    private static final String LLM_REQUESTS_TOPIC = "llm-request";
    
    @Override
    public void publishChatEvent(String chatRoomId, String eventType, String userId) {
        Map<String, Object> eventData = Map.of(
            "chatRoomId", chatRoomId,
            "eventType", eventType,
            "userId", userId
        );
        
        kafkaTemplate.send(CHAT_EVENTS_TOPIC, chatRoomId, eventData);
        log.info("Chat event published: {} for chat room: {}", eventType, chatRoomId);
    }

    
    @Override
    public void publishLlmRequest(LlmRequest llmRequest) {
        try {
            // MSA 원칙: 단순 JSON 문자열로 직렬화하여 서비스 간 독립성 보장
            Map<String, Object> requestData = Map.of(
                "chatRoomId", llmRequest.getChatRoomId(),
                "userId", llmRequest.getUserId(),
                "userMessage", llmRequest.getUserMessage(),
                "requestId", llmRequest.getRequestId()
            );
            
            kafkaTemplate.send(LLM_REQUESTS_TOPIC, llmRequest.getChatRoomId(), requestData);
            log.info("LLM request published to Kafka: {}", llmRequest.getRequestId());
        } catch (Exception e) {
            log.error("Failed to publish LLM request: {}", llmRequest.getRequestId(), e);
        }
    }
}
