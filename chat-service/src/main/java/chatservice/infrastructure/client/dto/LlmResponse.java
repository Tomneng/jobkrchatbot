package chatservice.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {
    private String response;
    private String chatRoomId;
    private String userId;
    private String requestId;
    private String error;
} 