package chatservice.infrastructure.adapter;

import chatservice.domain.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataChatRepository extends JpaRepository<ChatRoom, String> {
    
    List<ChatRoom> findByUserIdOrderByCreatedAtDesc(String userId);
    
    boolean existsByUserId(String userId);
}
