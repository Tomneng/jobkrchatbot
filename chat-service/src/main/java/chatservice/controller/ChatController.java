package chatservice.controller;

import chatservice.application.dto.ChatRoomResponse;
import chatservice.application.dto.StartChatRequest;
import chatservice.application.dto.*;
import chatservice.application.service.ChatApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatApplicationService chatApplicationService;
    
    @Value("${llm.service.url:http://localhost:8082}")
    private String llmServiceUrl;
    
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
    
    @PostMapping(value = "/message/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendStreamingMessage(@RequestBody SendMessageRequest request) {
        log.info("Sending streaming message to chat room: {}", request.getChatRoomId());
        return chatApplicationService.sendStreamingMessage(request);
    }
    
    // LLM 서비스 직접 접근을 위한 프록시 엔드포인트
    @GetMapping("/llm-service-url")
    public ResponseEntity<String> getLlmServiceUrl() {
        return ResponseEntity.ok(llmServiceUrl);
    }
    
    // 메시지 저장만 하는 엔드포인트
    @PostMapping("/save-message")
    public ResponseEntity<Void> saveMessage(@RequestBody SaveMessageRequest request) {
        log.info("Saving message for chat room: {}", request.getChatRoomId());
        chatApplicationService.saveMessage(request);
        return ResponseEntity.ok().build();
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