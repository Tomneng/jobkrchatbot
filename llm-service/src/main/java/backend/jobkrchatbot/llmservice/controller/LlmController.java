package backend.jobkrchatbot.llmservice.controller;

import backend.jobkrchatbot.llmservice.dto.LlmRequest;
import backend.jobkrchatbot.llmservice.dto.LlmResponse;
import backend.jobkrchatbot.llmservice.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @PostMapping("/generate")
    public ResponseEntity<LlmResponse> generateResponse(@RequestBody LlmRequest request) {
        log.info("Received LLM request for chat room: {}, user: {}", 
                request.getChatRoomId(), request.getUserId());
        
        try {
            LlmResponse response = llmService.generateResponse(request);
            
            if (response.getError() != null) {
                log.error("Error in LLM response: {}", response.getError());
                return ResponseEntity.internalServerError().body(response);
            }
            
            log.info("Successfully generated LLM response for request: {}", request.getRequestId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Unexpected error generating LLM response", e);
            
            LlmResponse errorResponse = LlmResponse.builder()
                    .error("서버 내부 오류가 발생했습니다.")
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
                    
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("LLM Service is running");
    }
}