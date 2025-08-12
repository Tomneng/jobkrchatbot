package backend.jobkrchatbot.chatservice.controller;

import backend.jobkrchatbot.common.dto.ChatRequest;
import backend.jobkrchatbot.common.dto.ChatResponse;
import backend.jobkrchatbot.common.response.ApiResponse;
import backend.jobkrchatbot.chatservice.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/start")
    public ApiResponse<ChatResponse> startChat() {
        String chatRoomId = UUID.randomUUID().toString();
        ChatResponse response = ChatResponse.builder()
                .message("안녕하세요! 취업 상담을 도와드리겠습니다. 무엇을 도와드릴까요?")
                .chatRoomId(chatRoomId)
                .build();
        return ApiResponse.success(response, "채팅이 시작되었습니다.");
    }

    @PostMapping("/{chatRoomId}/send")
    public ApiResponse<ChatResponse> sendMessage(@PathVariable String chatRoomId, @RequestBody ChatRequest request) {
        request.setChatRoomId(chatRoomId);
        request.setRequestId(UUID.randomUUID().toString());
        
        ChatResponse response = chatService.processChatMessage(request);
        return ApiResponse.success(response, "메시지가 성공적으로 처리되었습니다.");
    }

    @GetMapping("/{chatRoomId}/history")
    public ApiResponse<ChatResponse> getChatHistory(@PathVariable String chatRoomId) {
        ChatResponse response = chatService.getChatHistory(chatRoomId);
        return ApiResponse.success(response, "채팅 히스토리를 조회했습니다.");
    }

    @DeleteMapping("/{chatRoomId}")
    public ApiResponse<String> endChat(@PathVariable String chatRoomId) {
        chatService.endChat(chatRoomId);
        return ApiResponse.success("채팅방이 종료되었습니다.", "채팅이 종료되었습니다.");
    }
} 