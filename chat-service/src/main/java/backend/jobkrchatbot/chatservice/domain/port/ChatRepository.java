package backend.jobkrchatbot.chatservice.domain.port;

import backend.jobkrchatbot.chatservice.domain.model.ChatRoom;
import backend.jobkrchatbot.chatservice.domain.model.ChatRoomId;
import backend.jobkrchatbot.chatservice.domain.model.UserId;

import java.util.List;
import java.util.Optional;

public interface ChatRepository {
    
    ChatRoom save(ChatRoom chatRoom);
    
    Optional<ChatRoom> findById(ChatRoomId chatRoomId);
    
    List<ChatRoom> findByUserId(UserId userId);
    
    void delete(ChatRoomId chatRoomId);
    
    boolean existsById(ChatRoomId chatRoomId);
    
    List<ChatRoom> findActiveChatRooms();
} 