package chatservice.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatHistoryResponse {
    private String chatRoomId;
    private List<ChatMessageResponse> messages;
} 