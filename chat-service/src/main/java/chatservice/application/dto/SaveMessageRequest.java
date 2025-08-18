package chatservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveMessageRequest {
    private String chatRoomId;
    private String userId;
    private String message;
    private String sender; // "user" or "assistant"
} 