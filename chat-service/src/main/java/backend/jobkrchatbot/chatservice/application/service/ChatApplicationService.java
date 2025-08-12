package backend.jobkrchatbot.chatservice.application.service;

import backend.jobkrchatbot.chatservice.domain.model.*;
import backend.jobkrchatbot.chatservice.domain.port.ChatRepository;
import backend.jobkrchatbot.chatservice.domain.port.MessagePublisher;
import backend.jobkrchatbot.chatservice.domain.service.ChatDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatApplicationService {
    
    private final ChatRepository chatRepository;
    private final MessagePublisher messagePublisher;
    private final ChatDomainService chatDomainService;
    
    public ChatRoomResponse startChat(StartChatRequest request) {
        log.info("Starting chat for user: {}", request.getUserId());
        
        // 도메인 객체 생성 (생성자 사용)
        UserId userId = new UserId(request.getUserId());
        ResumeInfo resumeInfo = new ResumeInfo(
                request.getCareerSummary(),
                request.getJobRole(),
                request.getTechnicalSkills(),
                request.getExperience()
        );
        
        MbtiType mbtiType = MbtiType.fromString(request.getMbtiType());
        if (mbtiType == null) {
            throw new IllegalArgumentException("유효하지 않은 MBTI 타입입니다: " + request.getMbtiType());
        }
        
        ChatRoom chatRoom = chatDomainService.createChatRoom(userId, resumeInfo, mbtiType);
        
        // 채팅방 저장
        ChatRoom savedChatRoom = chatRepository.save(chatRoom);
        
        // 환영 메시지 생성 및 추가
        String welcomeMessage = chatDomainService.generateWelcomeMessage(savedChatRoom);
        ChatMessage welcomeMsg = new ChatMessage(savedChatRoom.getId(), MessageType.SYSTEM, welcomeMessage);
        
        savedChatRoom.addMessage(welcomeMsg);
        chatRepository.save(savedChatRoom);
        
        // 채팅 시작 이벤트 발행
        messagePublisher.publishChatEvent(savedChatRoom.getId(), "CHAT_STARTED", 
                Map.of("userId", request.getUserId(), "mbtiType", mbtiType.name()));
        
        return ChatRoomResponse.from(savedChatRoom);
    }
    
    public ChatResponse sendMessage(SendMessageRequest request) {
        log.info("Processing message for chat room: {}", request.getChatRoomId());
        
        ChatRoomId chatRoomId = new ChatRoomId(request.getChatRoomId());
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + request.getChatRoomId()));
        
        // 도메인 서비스를 통한 채팅 세션 검증
        chatDomainService.validateChatSession(chatRoom);
        
        // 사용자 메시지 생성 및 추가
        ChatMessage userMessage = new ChatMessage(chatRoomId, MessageType.USER, request.getMessage());
        
        chatRoom.addMessage(userMessage);
        chatRepository.save(chatRoom);
        
        // MBTI 기반 프롬프트 생성
        String mbtiBasedPrompt = chatDomainService.generateMbtiBasedPrompt(
                request.getMessage(), chatRoom.getMbtiType());
        
        // LLM 서비스로 요청 전송 (비동기)
        String requestId = UUID.randomUUID().toString();
        messagePublisher.publishUserMessage(chatRoomId, new MessageId(requestId), mbtiBasedPrompt);
        
        // 임시 응답 반환
        return ChatResponse.builder()
                .message("메시지를 처리 중입니다. 잠시만 기다려주세요...")
                .chatRoomId(request.getChatRoomId())
                .requestId(requestId)
                .build();
    }
    
    public ChatHistoryResponse getChatHistory(String chatRoomId) {
        ChatRoomId roomId = new ChatRoomId(chatRoomId);
        ChatRoom chatRoom = chatRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));
        
        List<ChatMessage> messages = chatRoom.getRecentMessages(50); // 최근 50개 메시지
        
        return ChatHistoryResponse.builder()
                .chatRoomId(chatRoomId)
                .messages(messages.stream()
                        .map(ChatMessageResponse::from)
                        .toList())
                .build();
    }
    
    public void endChat(String chatRoomId) {
        ChatRoomId roomId = new ChatRoomId(chatRoomId);
        ChatRoom chatRoom = chatRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));
        
        chatRoom.endChat();
        chatRepository.save(chatRoom);
        
        // 채팅 종료 이벤트 발행
        messagePublisher.publishChatEvent(roomId, "CHAT_ENDED", 
                Map.of("chatRoomId", chatRoomId, "messageCount", chatRoom.getMessageCount()));
        
        log.info("Chat room ended: {}, total messages: {}", chatRoomId, chatRoom.getMessageCount());
    }
    
    public List<ChatRoomSummaryResponse> getUserChatRooms(String userId) {
        UserId uid = new UserId(userId);
        List<ChatRoom> chatRooms = chatRepository.findByUserId(uid);
        
        return chatRooms.stream()
                .map(ChatRoomSummaryResponse::from)
                .toList();
    }
} 