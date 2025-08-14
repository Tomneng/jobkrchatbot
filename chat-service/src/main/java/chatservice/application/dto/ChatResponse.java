package chatservice.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
    private String message;
    private String chatRoomId;
    private String requestId;
} 