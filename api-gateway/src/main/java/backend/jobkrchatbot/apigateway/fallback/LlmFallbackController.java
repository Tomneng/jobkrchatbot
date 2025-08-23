package backend.jobkrchatbot.apigateway.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class LlmFallbackController {
    
    @GetMapping("/llm")
    public Mono<ResponseEntity<Map<String, Object>>> llmFallback() {
        log.warn("LLM Service is not available, using fallback response");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "LLM Service is temporarily unavailable. Please try again later.");
        response.put("error", "LLM_SERVICE_DOWN");
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
    
    @GetMapping("/chat")
    public Mono<ResponseEntity<Map<String, Object>>> chatFallback() {
        log.warn("Chat Service is not available, using fallback response");
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", "Chat Service is temporarily unavailable. Please try again later.");
        response.put("error", "CHAT_SERVICE_DOWN");
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
