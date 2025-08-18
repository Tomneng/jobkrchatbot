package chatservice.application.dto;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String chatRoomId;
    private String userId;
    private String message;
} 