package chatservice.domain.port;

import chatservice.infrastructure.client.dto.LlmRequest;

public interface MessagePublisher {
    void publishChatEvent(String chatRoomId, String eventType, String userId);
    void publishUserMessage(String chatRoomId, String requestId, String message);
    void publishLlmRequest(LlmRequest llmRequest);
} 