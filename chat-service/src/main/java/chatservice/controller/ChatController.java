package chatservice.controller;

import chatservice.application.dto.ChatRoomResponse;
import chatservice.application.dto.StartChatRequest;
import chatservice.application.dto.*;
import chatservice.application.service.ChatApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody SendMessageRequest request) {
        log.info("Sending message to chat room: {}", request.getChatRoomId());
        ChatResponse response = chatApplicationService.sendMessage(request);
        return ResponseEntity.ok(response);
    }
    
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
} 