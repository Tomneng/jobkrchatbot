package backend.jobkrchatbot.llmservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {
    private String chatRoomId;
    private String userId;
    private String userMessage;
    private String requestId;
} 