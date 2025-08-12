package backend.jobkrchatbot.chatservice.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ChatRoom {
    
    private final ChatRoomId id;
    private final UserId userId;
    private final ResumeInfo resumeInfo;
    private final MbtiType mbtiType;
    private ChatStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private final List<ChatMessage> messages;
    
    public ChatRoom(UserId userId, ResumeInfo resumeInfo, MbtiType mbtiType) {
        this.id = new ChatRoomId(UUID.randomUUID().toString());
        this.userId = userId;
        this.resumeInfo = resumeInfo;
        this.mbtiType = mbtiType;
        this.status = ChatStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
        this.messages = new ArrayList<>();
    }
    
    // 도메인 메서드들
    public void addMessage(ChatMessage message) {
        if (status != ChatStatus.ACTIVE) {
            throw new IllegalStateException("채팅방이 비활성 상태입니다.");
        }
        
        messages.add(message);
        lastActivityAt = LocalDateTime.now();
    }
    
    public void endChat() {
        this.status = ChatStatus.ENDED;
        this.lastActivityAt = LocalDateTime.now();
    }
    
    public boolean canAddMessage() {
        return status == ChatStatus.ACTIVE;
    }
    
    // Getter 메서드들
    public ChatRoomId getId() { return id; }
    public UserId getUserId() { return userId; }
    public ResumeInfo getResumeInfo() { return resumeInfo; }
    public MbtiType getMbtiType() { return mbtiType; }
    public ChatStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    
    public int getMessageCount() {
        return messages.size();
    }
    
    public ChatMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }
    
    public List<ChatMessage> getRecentMessages(int count) {
        if (messages.size() <= count) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(messages.size() - count, messages.size()));
    }
    
    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }
} 