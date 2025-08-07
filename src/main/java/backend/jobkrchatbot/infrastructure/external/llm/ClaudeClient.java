package backend.jobkrchatbot.infrastructure.external.llm;

import backend.jobkrchatbot.domain.model.LlmPrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeClient implements LlmClient {

    private final RestTemplate restTemplate;

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.url}")
    private String apiUrl;

    @Value("${anthropic.api.model}")
    private String model;

    @Override
    public String generateResponse(String prompt, String systemMessage) {
        return callClaudeApi(prompt, systemMessage, 1000, 0.7);
    }

    private String callClaudeApi(String userPrompt, String systemPrompt, int maxTokens, double temperature) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", temperature,
                "messages", List.of(
                    Map.of("role", "user", "content", systemPrompt + "\n\n" + userPrompt)
                )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);

            return extractContentFromResponse(response);

        } catch (Exception e) {
            log.error("Error calling Claude API", e);
            return "API 호출 중 오류가 발생했습니다.";
        }
    }

    private String extractContentFromResponse(Map<String, Object> response) {
        if (response != null && response.containsKey("content")) {
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (!content.isEmpty()) {
                Map<String, Object> firstContent = content.get(0);
                return (String) firstContent.get("text");
            }
        }

        log.error("Failed to get response from Claude API: {}", response);
        return "응답 생성 중 오류가 발생했습니다.";
    }
} 