package chatservice.domain.port;

import chatservice.infrastructure.client.dto.LlmRequest;

public interface MessagePublisher {
    void publishChatEvent(String chatRoomId, String eventType, String userId);
    void publishLlmRequest(LlmRequest llmRequest);
} 