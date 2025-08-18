package backend.jobkrchatbot.llmservice.controller;

import backend.jobkrchatbot.llmservice.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    /**
     * 채팅방별 직접 SSE 스트리밍 연결
     */
    @GetMapping(value = "/stream/{chatRoomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@PathVariable String chatRoomId) {
        log.info("Creating SSE stream for chat room: {}", chatRoomId);
        return llmService.createStreamingConnection(chatRoomId);
    }
}
