package chatservice.domain.port;

public interface MessagePublisher {
    void publishChatEvent(String chatRoomId, String eventType, String userId);
    void publishUserMessage(String chatRoomId, String requestId, String message);
} 