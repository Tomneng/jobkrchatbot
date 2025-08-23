package backend.jobkrchatbot.apigateway.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class LlmFallbackController {
    
    @GetMapping("/llm")
    public ResponseEntity<String> llmFallback() {
        log.warn("LLM Service is not available, using fallback response");
        return ResponseEntity.ok("LLM Service is temporarily unavailable. Please try again later.");
    }
}
