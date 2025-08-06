package backend.jobkrchatbot.presentation.controller;

import backend.jobkrchatbot.application.dto.ChatRequest;
import backend.jobkrchatbot.application.dto.ChatResponse;
import backend.jobkrchatbot.application.service.ChatService;
import backend.jobkrchatbot.common.response.ApiResponse;
import backend.jobkrchatbot.domain.model.FirstChatPrompt;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final FirstChatPrompt firstChatPrompt;

    /**
     * 초기 채팅 시작 - 첫 채팅 문구 반환 및 채팅방 ID 생성
     */
    @GetMapping("/start")
    public ApiResponse<ChatResponse> startChat() {
        String chatRoomId = UUID.randomUUID().toString();
        ChatResponse response = ChatResponse.builder()
                .message(firstChatPrompt.getSystemPrompt())
                .chatRoomId(chatRoomId)
                .build();
        return ApiResponse.success(response, "채팅이 시작되었습니다.");
    }

    /**
     * 채팅 메시지 전송 - 채팅방 ID로 구별
     */
    @PostMapping("/{chatRoomId}/send")
    public ApiResponse<ChatResponse> sendMessage(@PathVariable String chatRoomId, @RequestBody ChatRequest request) {
        ChatResponse response = chatService.generateChatResponse(request, chatRoomId);
        return ApiResponse.success(response, "메시지가 성공적으로 처리되었습니다.");
    }

    /**
     * 채팅방 히스토리 조회
     */
    @GetMapping("/{chatRoomId}/history")
    public ApiResponse<ChatResponse> getChatHistory(@PathVariable String chatRoomId) {
        ChatResponse response = chatService.getChatHistory(chatRoomId);
        return ApiResponse.success(response, "채팅 히스토리를 조회했습니다.");
    }

    /**
     * 채팅방 종료
     */
    @DeleteMapping("/{chatRoomId}")
    public ApiResponse<String> endChat(@PathVariable String chatRoomId) {
        chatService.endChat(chatRoomId);
        return ApiResponse.success("채팅방이 종료되었습니다.", "채팅이 종료되었습니다.");
    }
} 