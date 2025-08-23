package chatservice.domain.port;

import chatservice.domain.model.ChatMessage;
import chatservice.domain.model.ChatRoom;
import java.util.List;
import java.util.Optional;

public interface ChatRepository {
    ChatRoom save(ChatRoom chatRoom);
    Optional<ChatRoom> findById(String id);
    List<ChatRoom> findByUserId(String userId);

    List<ChatMessage> findAllChat();
} 