package chatservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_rooms")
public class ChatRoom {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages;
    
    public ChatRoom(String userId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.messages = new ArrayList<>();
    }
    
    // 도메인 메서드들
    public void addMessage(ChatMessage message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
    }
    
    public int getMessageCount() {
        return messages != null ? messages.size() : 0;
    }
    
    public List<ChatMessage> getRecentMessages(int count) {
        if (messages == null || messages.size() <= count) {
            return new ArrayList<>(messages != null ? messages : List.of());
        }
        return new ArrayList<>(messages.subList(messages.size() - count, messages.size()));
    }
}