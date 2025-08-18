package chatservice.infrastructure.client;

import chatservice.infrastructure.client.dto.LlmRequest;
import chatservice.infrastructure.client.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final WebClient webClient;
    
    @Value("${llm.service.url:http://localhost:8082}")
    private String llmServiceUrl;

    public LlmResponse generateResponse(LlmRequest request) {
        try {
            String url = llmServiceUrl + "/api/llm/generate";
            
            log.info("Calling LLM service at: {} for chat room: {}", url, request.getChatRoomId());
            
            Mono<LlmResponse> responseMono = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LlmResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .doOnSuccess(response -> {
                        if (response != null && response.getError() == null) {
                            log.info("Successfully received LLM response for request: {}", request.getRequestId());
                        } else {
                            log.error("LLM service returned error: {}", response != null ? response.getError() : "null response");
                        }
                    })
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("HTTP error calling LLM service: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Mono.just(createErrorResponse(request, "HTTP 오류: " + ex.getStatusCode() + " - " + ex.getMessage()));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        log.error("Error calling LLM service for request: {}", request.getRequestId(), ex);
                        return Mono.just(createErrorResponse(request, "LLM 서비스 호출 중 오류가 발생했습니다: " + ex.getMessage()));
                    });
            
            // block()을 사용하여 동기적으로 결과 반환
            return responseMono.block();
            
        } catch (Exception e) {
            log.error("Unexpected error calling LLM service for request: {}", request.getRequestId(), e);
            return createErrorResponse(request, "예상치 못한 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    public Flux<ServerSentEvent<String>> generateStreamingResponse(LlmRequest request) {
        try {
            String url = llmServiceUrl + "/api/llm/stream";
            
            log.info("Calling streaming LLM service at: {} for chat room: {}", url, request.getChatRoomId());
            
            return webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                    .timeout(Duration.ofSeconds(60))
                    .doOnSubscribe(subscription -> 
                        log.info("Started streaming for request: {}", request.getRequestId()))
                    .doOnComplete(() -> 
                        log.info("Completed streaming for request: {}", request.getRequestId()))
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("HTTP error in streaming LLM service: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("error")
                                .data("HTTP 오류: " + ex.getStatusCode())
                                .build());
                    })
                    .onErrorResume(Exception.class, ex -> {
                        log.error("Error in streaming LLM service for request: {}", request.getRequestId(), ex);
                        return Flux.just(ServerSentEvent.<String>builder()
                                .event("error")
                                .data("스트리밍 중 오류가 발생했습니다: " + ex.getMessage())
                                .build());
                    });
            
        } catch (Exception e) {
            log.error("Unexpected error calling streaming LLM service for request: {}", request.getRequestId(), e);
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("예상치 못한 오류가 발생했습니다: " + e.getMessage())
                    .build());
        }
    }
    
    private LlmResponse createErrorResponse(LlmRequest request, String errorMessage) {
        return LlmResponse.builder()
                .error(errorMessage)
                .chatRoomId(request.getChatRoomId())
                .userId(request.getUserId())
                .requestId(request.getRequestId())
                .build();
    }

    /**
     * LLM 서비스에 SSE Emitter 등록
     */
    public void registerEmitter(String chatRoomId) {
        try {
            String url = llmServiceUrl + "/api/llm/register-emitter/" + chatRoomId;
            log.info("Registering SSE emitter with LLM service: {}", url);
            
            // POST 요청으로 SSE Emitter 등록
            // Note: This part of the code was not provided in the original file,
            // so it's added as a placeholder. In a real application, you would
            // use a WebClient or RestTemplate to make this call.
            // For now, we'll just log the attempt.
            log.info("Attempting to register SSE emitter for chat room: {}", chatRoomId);
            
        } catch (Exception e) {
            log.error("Failed to register SSE emitter with LLM service for chat room: {}", chatRoomId, e);
            throw new RuntimeException("LLM 서비스에 SSE Emitter 등록 실패", e);
        }
    }
} 