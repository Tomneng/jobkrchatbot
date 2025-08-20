package backend.jobkrchatbot.llmservice.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    @Value("${claude.api.model}")
    private String model;

    @Value("${claude.api.version}")
    private String apiVersion;

    public String generateResponse(String userPrompt, String systemPrompt) {
        return callClaudeApi(userPrompt, systemPrompt, 2000).block();
    }

    /**
     * Claude API 스트리밍 응답 생성 (응답 파싱만 담당)
     */
    public Flux<String> generateStreamingResponse(String userPrompt, String systemPrompt) {
        try {
            log.info("Claude API 스트리밍 시작 - 프롬프트: {}", userPrompt.substring(0, Math.min(50, userPrompt.length())));
            log.info("Claude API 설정 - URL: {}, Model: {}, Version: {}", apiUrl, model, apiVersion);
            log.info("Claude API Key 설정됨: {}", apiKey != null && !apiKey.isEmpty() ? "YES" : "NO");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", apiVersion);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 2000,
                "stream", true,
                "system", systemPrompt,
                "messages", List.of(
                    Map.of("role", "user", "content", userPrompt)
                )
            );
            
            log.info("Claude API 요청 Body: {}", requestBody);

            return webClient.post()
                .uri(apiUrl)
                .headers(h -> h.putAll(headers))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(rawLine -> log.debug("Raw streaming line: {}", rawLine))
                .filter(data -> !data.equals("[DONE]"))
                    .map(data -> {
                        try {
                            JsonNode json = objectMapper.readTree(data);

                            // Claude 응답 구조: {"type": "content_block_delta", "delta": {"text": "..."}}
                            if (json.has("type") && "content_block_delta".equals(json.get("type").asText())) {
                                if (json.has("delta") && json.get("delta").has("text")) {
                                    String content = json.get("delta").get("text").asText();
                                    return content;
                                }
                            }

                            return "";

                        } catch (Exception e) {
                            log.error("파싱 실패: {}", data, e);
                            return "";
                        }
                    })
                .filter(content -> content != null && !content.isEmpty())
                .doOnComplete(() -> log.info("Claude API 응답 파싱 완료"));

        } catch (Exception e) {
            log.error("Error setting up Claude API streaming", e);
            return Flux.error(e);
        }
    }

    private Mono<String> callClaudeApi(String userPrompt, String systemPrompt, int maxTokens) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", apiVersion);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(
                    Map.of("role", "user", "content", userPrompt)
                )
            );

            return webClient.post()
                .uri(apiUrl)
                .headers(h -> h.putAll(headers))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractContentFromResponse)
                .onErrorReturn("API 호출 중 오류가 발생했습니다.");

        } catch (Exception e) {
            log.error("Error calling Claude API", e);
            return Mono.just("API 호출 중 오류가 발생했습니다.");
        }
    }

    private String extractContentFromResponse(Map<String, Object> response) {
        if (response != null && response.containsKey("content")) {
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (!content.isEmpty()) {
                Map<String, Object> firstContent = content.get(0);
                if ("text".equals(firstContent.get("type"))) {
                    return (String) firstContent.get("text");
                }
            }
        }

        log.error("Failed to get response from Claude API: {}", response);
        return "응답 생성 중 오류가 발생했습니다.";
    }
} 