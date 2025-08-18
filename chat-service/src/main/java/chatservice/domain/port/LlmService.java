package chatservice.domain.port;

public interface LlmService {
    String generateResponse(String chatRoomId, String userId, String userMessage, String requestId);
} 