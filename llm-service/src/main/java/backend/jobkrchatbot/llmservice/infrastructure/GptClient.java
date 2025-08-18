package backend.jobkrchatbot.llmservice.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GptClient {

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    public String generateResponse(String userPrompt, String systemPrompt) {
        return callGptApi(userPrompt, systemPrompt, 2000, 0.3);
    }

    /**
     * GPT API 스트리밍 응답 생성 (응답 파싱만 담당)
     */
    public Flux<String> generateStreamingResponse(String userPrompt, String systemPrompt) {
        try {
            log.info("GPT API 스트리밍 시작 - 프롬프트: {}", userPrompt.substring(0, Math.min(50, userPrompt.length())));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 2000,
                "temperature", 0.3,
                "stream", true,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                )
            );

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
                            log.info("파싱 시작 - data: {}", data);
                            JsonNode json = objectMapper.readTree(data);
                            log.info("JSON 파싱 성공");

                            // choices 배열 확인
                            if (!json.has("choices") || json.get("choices").size() == 0) {
                                log.info("choices 배열 문제");
                                return null;
                            }

                            JsonNode choice = json.get("choices").get(0);
                            log.info("choice: {}", choice);

                            // finish_reason 확인
                            if (choice.has("finish_reason") && !choice.get("finish_reason").asText().equals("null")) {
                                log.info("finish_reason 조건에서 걸림");
                                return null;
                            }

                            // delta에서 content 추출
                            if (choice.has("delta") && choice.get("delta").has("content")) {
                                String content = choice.get("delta").get("content").asText();
                                log.info("content 추출 성공: '{}'", content);
                                return content;
                            }

                            log.info("delta에 content가 없음");
                            return null;

                        } catch (Exception e) {
                            log.error("파싱 실패: {}", data, e);
                            return null;
                        }
                    })
                .filter(content -> content != null && !content.isEmpty())
                .doOnComplete(() -> log.info("GPT API 응답 파싱 완료"));

        } catch (Exception e) {
            log.error("Error setting up GPT API streaming", e);
            return Flux.error(e);
        }
    }

    private String callGptApi(String userPrompt, String systemPrompt, int maxTokens, double temperature) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", temperature,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);

            return extractContentFromResponse(response);

        } catch (Exception e) {
            log.error("Error calling GPT API", e);
            return "API 호출 중 오류가 발생했습니다.";
        }
    }

    private String extractContentFromResponse(Map<String, Object> response) {
        if (response != null && response.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                return (String) message.get("content");
            }
        }

        log.error("Failed to get response from GPT API: {}", response);
        return "응답 생성 중 오류가 발생했습니다.";
    }
}
