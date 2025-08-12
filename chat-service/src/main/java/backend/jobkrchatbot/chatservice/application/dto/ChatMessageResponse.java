package backend.jobkrchatbot.chatservice.application.dto;

import backend.jobkrchatbot.chatservice.domain.model.ChatMessage;
import backend.jobkrchatbot.chatservice.domain.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private String messageId;
    private String chatRoomId;
    private String type;
    private String content;
    private LocalDateTime createdAt;
    private String status;
    
    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId().toString())
                .chatRoomId(message.getChatRoomId().toString())
                .type(message.getType().name())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .status(message.getStatus().name())
                .build();
    }
} 