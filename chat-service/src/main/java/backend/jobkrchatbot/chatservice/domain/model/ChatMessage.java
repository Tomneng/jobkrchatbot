package backend.jobkrchatbot.chatservice.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatMessage {
    
    private final MessageId id;
    private final ChatRoomId chatRoomId;
    private final MessageType type;
    private final String content;
    private final LocalDateTime createdAt;
    private MessageStatus status;
    
    public ChatMessage(ChatRoomId chatRoomId, MessageType type, String content) {
        this.id = new MessageId(UUID.randomUUID().toString());
        this.chatRoomId = chatRoomId;
        this.type = type;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.status = MessageStatus.SENT;
    }
    
    // 도메인 메서드들
    public void markAsProcessed() {
        this.status = MessageStatus.PROCESSED;
    }
    
    public void markAsFailed() {
        this.status = MessageStatus.FAILED;
    }
    
    public boolean isUserMessage() {
        return type == MessageType.USER;
    }
    
    public boolean isAssistantMessage() {
        return type == MessageType.ASSISTANT;
    }
    
    public boolean isSystemMessage() {
        return type == MessageType.SYSTEM;
    }
    
    // Getter 메서드들
    public MessageId getId() { return id; }
    public ChatRoomId getChatRoomId() { return chatRoomId; }
    public MessageType getType() { return type; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public MessageStatus getStatus() { return status; }
} 