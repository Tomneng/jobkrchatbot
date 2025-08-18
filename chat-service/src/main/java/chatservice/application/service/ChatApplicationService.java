package chatservice.application.service;

import chatservice.domain.model.ChatMessage;
import chatservice.domain.model.ChatRoom;
import chatservice.domain.port.ChatRepository;
import chatservice.domain.port.MessagePublisher;
import chatservice.application.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatApplicationService {
    
    private final ChatRepository chatRepository;
    private final MessagePublisher messagePublisher;
    
    public ChatRoomResponse startChat(StartChatRequest request) {
        log.info("Starting chat for user: {}", request.getUserId());
        
        // 채팅방 생성
        ChatRoom chatRoom = new ChatRoom(request.getUserId());
        
        // 채팅방 저장
        ChatRoom savedChatRoom = chatRepository.save(chatRoom);
        
        // 채팅 시작 이벤트 발행
        messagePublisher.publishChatEvent(savedChatRoom.getId(), "CHAT_STARTED", 
                request.getUserId());
        
        return ChatRoomResponse.from(savedChatRoom);
    }
    
    public ChatResponse sendMessage(SendMessageRequest request) {
        log.info("Processing message for chat room: {}", request.getChatRoomId());
        
        ChatRoom chatRoom = chatRepository.findById(request.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + request.getChatRoomId()));
        
        // 사용자 메시지 생성 및 추가
        ChatMessage userMessage = new ChatMessage(
            request.getChatRoomId(), 
            request.getUserId(), 
            request.getMessage()
        );
        
        chatRoom.addMessage(userMessage);
        // 메시지가 추가된 채팅방을 저장
        chatRepository.save(chatRoom);
        
        // LLM 서비스로 요청 전송 (비동기)
        String requestId = UUID.randomUUID().toString();
        messagePublisher.publishUserMessage(request.getChatRoomId(), requestId, request.getMessage());
        
        // 임시 응답 반환
        return ChatResponse.builder()
                .message("메시지를 처리 중입니다. 잠시만 기다려주세요...")
                .chatRoomId(request.getChatRoomId())
                .requestId(requestId)
                .build();
    }
    
    public ChatHistoryResponse getChatHistory(String chatRoomId) {
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));
        
        List<ChatMessage> messages = chatRoom.getRecentMessages(50); // 최근 50개 메시지
        
        return ChatHistoryResponse.builder()
                .chatRoomId(chatRoomId)
                .messages(messages.stream()
                        .map(ChatMessageResponse::from)
                        .toList())
                .build();
    }
    
    public List<ChatRoomSummaryResponse> getUserChatRooms(String userId) {
        List<ChatRoom> chatRooms = chatRepository.findByUserId(userId);
        
        return chatRooms.stream()
                .map(ChatRoomSummaryResponse::from)
                .toList();
    }
} 