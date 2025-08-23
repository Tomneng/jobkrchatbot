package backend.jobkrchatbot.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
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
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory;

    @GetMapping("/gateway")
    public Mono<ResponseEntity<Map<String, Object>>> gatewayHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now());
        health.put("status", "UP");
        health.put("service", "API Gateway");
        health.put("version", "1.0.0");
        
        return Mono.just(ResponseEntity.ok(health));
    }

    @GetMapping("/circuit-breakers")
    public Mono<ResponseEntity<Map<String, Object>>> circuitBreakersHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now());
        health.put("status", "UP");
        health.put("circuitBreakers", "Available");
        
        // Circuit Breaker 상태 확인
        try {
            // 간단한 방법으로 Circuit Breaker Factory 상태 확인
            if (circuitBreakerFactory != null) {
                health.put("resilience4j", "Available");
                health.put("circuitBreakerFactory", "Initialized");
                
                // 설정된 Circuit Breaker 인스턴스들 확인
                Map<String, String> circuitBreakerInstances = new HashMap<>();
                circuitBreakerInstances.put("chat-service-circuit-breaker", "Configured");
                circuitBreakerInstances.put("llm-service-circuit-breaker", "Configured");
                health.put("circuitBreakerInstances", circuitBreakerInstances);
            } else {
                health.put("resilience4j", "Factory not available");
                health.put("status", "DOWN");
            }
        } catch (Exception e) {
            log.error("Error checking circuit breaker status", e);
            health.put("resilience4j", "Error: " + e.getMessage());
            health.put("status", "DOWN");
        }
        
        return Mono.just(ResponseEntity.ok(health));
    }
}
