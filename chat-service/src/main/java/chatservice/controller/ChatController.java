package chatservice.controller;

import chatservice.application.dto.ChatRoomResponse;
import chatservice.application.dto.StartChatRequest;
import chatservice.application.dto.*;
import chatservice.application.service.ChatApplicationService;
import chatservice.domain.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatApplicationService chatApplicationService;
    

    
    @PostMapping("/start")
    public ResponseEntity<ChatRoomResponse> startChat(@RequestBody StartChatRequest request) {
        log.info("Starting chat for user: {}", request.getUserId());
        ChatRoomResponse response = chatApplicationService.startChat(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/message/stream")
    public ResponseEntity<Void> sendStreamingMessage(@RequestBody SendMessageRequest request) {
        log.info("Sending streaming message to chat room: {}", request.getChatRoomId());
        chatApplicationService.sendStreamingMessage(request);
        return ResponseEntity.ok().build();
    }
    
    // LLM 서비스 URL은 이제 API Gateway를 통해 접근
    // 기존 프록시 엔드포인트는 API Gateway로 이동됨

    
    @GetMapping("/{chatRoomId}/history")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(@PathVariable String chatRoomId) {
        log.info("Getting chat history for room: {}", chatRoomId);
        ChatHistoryResponse response = chatApplicationService.getChatHistory(chatRoomId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}/rooms")
    public ResponseEntity<List<ChatRoomSummaryResponse>> getUserChatRooms(@PathVariable String userId) {
        log.info("Getting chat rooms for user: {}", userId);
        List<ChatRoomSummaryResponse> response = chatApplicationService.getUserChatRooms(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all/messages")
    public ResponseEntity<List<ChatMessage>> getAllChatMessages() {
        log.info("Getting all chat messages");
        List<ChatMessage> response = chatApplicationService.getAllChatMessages();
        return ResponseEntity.ok(response);
    }
} 