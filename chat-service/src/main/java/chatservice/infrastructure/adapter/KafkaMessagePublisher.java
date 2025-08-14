package chatservice.infrastructure.adapter;

import chatservice.domain.port.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessagePublisher implements MessagePublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String CHAT_EVENTS_TOPIC = "chat-events";
    private static final String USER_MESSAGES_TOPIC = "user-messages";
    
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
}
