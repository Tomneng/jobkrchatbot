package chatservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "chat_room_id", nullable = false)
    private String chatRoomId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", insertable = false, updatable = false)
    private ChatRoom chatRoom;
    
    public ChatMessage(String chatRoomId, String userId, String content) {
        this.id = UUID.randomUUID().toString();
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
} 