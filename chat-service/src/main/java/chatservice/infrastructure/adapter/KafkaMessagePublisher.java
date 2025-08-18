package chatservice.infrastructure.adapter;

import chatservice.domain.port.MessagePublisher;
import chatservice.infrastructure.client.dto.LlmRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessagePublisher implements MessagePublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CHAT_EVENTS_TOPIC = "chat-events";
    private static final String USER_MESSAGES_TOPIC = "user-messages";
    private static final String LLM_REQUESTS_TOPIC = "llm-request";
    
    @Override
    public void publishChatEvent(String chatRoomId, String eventType, String userId) {
        String eventMessage = String.format("{\"chatRoomId\":\"%s\",\"eventType\":\"%s\",\"userId\":\"%s\"}", 
                chatRoomId, eventType, userId);
        
        kafkaTemplate.send(CHAT_EVENTS_TOPIC, chatRoomId, eventMessage);
        log.info("Chat event published: {} for chat room: {}", eventType, chatRoomId);
    }
    
    @Override
    public void publishUserMessage(String chatRoomId, String requestId, String message) {
        String messagePayload = String.format("{\"chatRoomId\":\"%s\",\"requestId\":\"%s\",\"message\":\"%s\"}", 
                chatRoomId, requestId, message);
        
        kafkaTemplate.send(USER_MESSAGES_TOPIC, chatRoomId, messagePayload);
        log.info("User message published for chat room: {}, requestId: {}", chatRoomId, requestId);
    }
    
    @Override
    public void publishLlmRequest(LlmRequest llmRequest) {
        try {
            String requestJson = objectMapper.writeValueAsString(llmRequest);
            kafkaTemplate.send(LLM_REQUESTS_TOPIC, llmRequest.getChatRoomId(), requestJson);
            log.info("LLM request published to Kafka: {}", llmRequest.getRequestId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize LLM request: {}", llmRequest.getRequestId(), e);
        }
    }
}
