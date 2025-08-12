package backend.jobkrchatbot.chatservice.application.dto;

import backend.jobkrchatbot.chatservice.domain.model.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomSummaryResponse {
    private String chatRoomId;
    private String userId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private int messageCount;
    
    public static ChatRoomSummaryResponse from(ChatRoom chatRoom) {
        return ChatRoomSummaryResponse.builder()
                .chatRoomId(chatRoom.getId().toString())
                .userId(chatRoom.getUserId().toString())
                .status(chatRoom.getStatus().name())
                .createdAt(chatRoom.getCreatedAt())
                .lastActivityAt(chatRoom.getLastActivityAt())
                .messageCount(chatRoom.getMessageCount())
                .build();
    }
} 