package chatservice.application.dto;

import chatservice.domain.model.ChatMessage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private String id;
    private String userId;
    private String content;
    private LocalDateTime createdAt;
    
    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .userId(message.getUserId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
} 