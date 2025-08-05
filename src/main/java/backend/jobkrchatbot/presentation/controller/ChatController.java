package backend.jobkrchatbot.presentation.controller;

import backend.jobkrchatbot.application.dto.ChatRequest;
import backend.jobkrchatbot.application.dto.ChatResponse;
import backend.jobkrchatbot.application.service.ChatService;
import backend.jobkrchatbot.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public ApiResponse<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.generateChatResponse(request);
        return ApiResponse.success(response, "메시지가 성공적으로 처리되었습니다.");
    }
} 