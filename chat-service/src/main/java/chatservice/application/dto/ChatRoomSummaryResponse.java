package chatservice.application.dto;

import chatservice.domain.model.ChatRoom;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatRoomSummaryResponse {
    private String id;
    private String userId;
    private LocalDateTime createdAt;
    private int messageCount;
    
    public static ChatRoomSummaryResponse from(ChatRoom chatRoom) {
        return ChatRoomSummaryResponse.builder()
                .id(chatRoom.getId())
                .userId(chatRoom.getUserId())
                .createdAt(chatRoom.getCreatedAt())
                .messageCount(chatRoom.getMessageCount())
                .build();
    }
} 