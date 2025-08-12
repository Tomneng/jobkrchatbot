package backend.jobkrchatbot.chatservice.domain.port;

import backend.jobkrchatbot.chatservice.domain.model.ChatRoomId;
import backend.jobkrchatbot.chatservice.domain.model.MessageId;

public interface MessagePublisher {
    
    void publishUserMessage(ChatRoomId chatRoomId, MessageId messageId, String userMessage);
    
    void publishSystemMessage(ChatRoomId chatRoomId, String systemMessage);
    
    void publishChatEvent(ChatRoomId chatRoomId, String eventType, Object eventData);
} 