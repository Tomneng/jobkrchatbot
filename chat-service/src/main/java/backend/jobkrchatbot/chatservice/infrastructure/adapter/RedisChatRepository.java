package backend.jobkrchatbot.chatservice.infrastructure.adapter;

import backend.jobkrchatbot.chatservice.domain.model.ChatRoom;
import backend.jobkrchatbot.chatservice.domain.model.ChatRoomId;
import backend.jobkrchatbot.chatservice.domain.model.UserId;
import backend.jobkrchatbot.chatservice.domain.port.ChatRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatRepository implements ChatRepository {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String CHAT_ROOM_KEY_PREFIX = "chat:room:";
    private static final String USER_CHAT_ROOMS_KEY_PREFIX = "user:chatrooms:";
    private static final String ACTIVE_CHAT_ROOMS_KEY = "chat:rooms:active";
    private static final Duration CHAT_ROOM_TTL = Duration.ofHours(24); // 24시간
    
    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        try {
            String chatRoomKey = CHAT_ROOM_KEY_PREFIX + chatRoom.getId().toString();
            String userChatRoomsKey = USER_CHAT_ROOMS_KEY_PREFIX + chatRoom.getUserId().toString();
            
            // 채팅방 정보 저장
            String chatRoomJson = objectMapper.writeValueAsString(chatRoom);
            redisTemplate.opsForValue().set(chatRoomKey, chatRoomJson, CHAT_ROOM_TTL);
            
            // 사용자별 채팅방 목록에 추가
            redisTemplate.opsForSet().add(userChatRoomsKey, chatRoom.getId().toString());
            redisTemplate.expire(userChatRoomsKey, CHAT_ROOM_TTL);
            
            // 활성 채팅방 목록에 추가
            if (chatRoom.getStatus().name().equals("ACTIVE")) {
                redisTemplate.opsForSet().add(ACTIVE_CHAT_ROOMS_KEY, chatRoom.getId().toString());
            }
            
            log.info("Chat room saved to Redis: {}", chatRoom.getId().toString());
            return chatRoom;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize chat room", e);
            throw new RuntimeException("채팅방 저장 실패", e);
        }
    }
    
    @Override
    public Optional<ChatRoom> findById(ChatRoomId chatRoomId) {
        try {
            String chatRoomKey = CHAT_ROOM_KEY_PREFIX + chatRoomId.toString();
            String chatRoomJson = redisTemplate.opsForValue().get(chatRoomKey);
            
            if (chatRoomJson == null) {
                return Optional.empty();
            }
            
            ChatRoom chatRoom = objectMapper.readValue(chatRoomJson, ChatRoom.class);
            return Optional.of(chatRoom);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize chat room", e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<ChatRoom> findByUserId(UserId userId) {
        try {
            String userChatRoomsKey = USER_CHAT_ROOMS_KEY_PREFIX + userId.toString();
            var chatRoomIds = redisTemplate.opsForSet().members(userChatRoomsKey);
            
            if (chatRoomIds == null || chatRoomIds.isEmpty()) {
                return List.of();
            }
            
            return chatRoomIds.stream()
                    .map(chatRoomId -> findById(new ChatRoomId(chatRoomId)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Failed to find chat rooms by user ID", e);
            return List.of();
        }
    }
    
    @Override
    public void delete(ChatRoomId chatRoomId) {
        try {
            String chatRoomKey = CHAT_ROOM_KEY_PREFIX + chatRoomId.toString();
            String chatRoomJson = redisTemplate.opsForValue().get(chatRoomKey);
            
            if (chatRoomJson != null) {
                ChatRoom chatRoom = objectMapper.readValue(chatRoomJson, ChatRoom.class);
                
                // 사용자별 채팅방 목록에서 제거
                String userChatRoomsKey = USER_CHAT_ROOMS_KEY_PREFIX + chatRoom.getUserId().toString();
                redisTemplate.opsForSet().remove(userChatRoomsKey, chatRoomId.toString());
                
                // 활성 채팅방 목록에서 제거
                redisTemplate.opsForSet().remove(ACTIVE_CHAT_ROOMS_KEY, chatRoomId.toString());
            }
            
            // 채팅방 정보 삭제
            redisTemplate.delete(chatRoomKey);
            log.info("Chat room deleted from Redis: {}", chatRoomId.toString());
            
        } catch (Exception e) {
            log.error("Failed to delete chat room", e);
            throw new RuntimeException("채팅방 삭제 실패", e);
        }
    }
    
    @Override
    public boolean existsById(ChatRoomId chatRoomId) {
        String chatRoomKey = CHAT_ROOM_KEY_PREFIX + chatRoomId.toString();
        return Boolean.TRUE.equals(redisTemplate.hasKey(chatRoomKey));
    }
    
    @Override
    public List<ChatRoom> findActiveChatRooms() {
        try {
            var activeChatRoomIds = redisTemplate.opsForSet().members(ACTIVE_CHAT_ROOMS_KEY);
            
            if (activeChatRoomIds == null || activeChatRoomIds.isEmpty()) {
                return List.of();
            }
            
            return activeChatRoomIds.stream()
                    .map(chatRoomId -> findById(new ChatRoomId(chatRoomId)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Failed to find active chat rooms", e);
            return List.of();
        }
    }
} 