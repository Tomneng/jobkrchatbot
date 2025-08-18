package chatservice.application.dto;

import chatservice.domain.model.ChatRoom;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatRoomResponse {
    private String id;
    private String userId;
    private LocalDateTime createdAt;
    
    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .userId(chatRoom.getUserId())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }
} 