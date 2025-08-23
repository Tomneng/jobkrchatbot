package chatservice.infrastructure.adapter;

import chatservice.domain.model.ChatMessage;
import chatservice.domain.model.ChatRoom;
import chatservice.domain.port.ChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JpaChatRepository implements ChatRepository {
    
    private final SpringDataChatRepository chatRepository;
    
    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        ChatRoom savedChatRoom = chatRepository.save(chatRoom);
        log.info("Chat room saved to database: {}", savedChatRoom.getId());
        return savedChatRoom;
    }
    
    @Override
    public Optional<ChatRoom> findById(String id) {
        return chatRepository.findById(id);
    }
    
    @Override
    public List<ChatRoom> findByUserId(String userId) {
        return chatRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<ChatMessage> findAllChat() {
        List<ChatRoom> allChatRooms = chatRepository.findAll();
        
        return allChatRooms.stream()
                .filter(chatRoom -> chatRoom.getMessages() != null)
                .flatMap(chatRoom -> chatRoom.getMessages().stream())
                .toList();
    }
}
