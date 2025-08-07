package backend.jobkrchatbot.infrastructure.external.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpLlmService {

    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    public String callLlmApi(String userPrompt, String systemPrompt, int maxTokens, double temperature) {
        try {
            HttpHeaders headers = createHeaders();
            Map<String, Object> requestBody = createRequestBody(userPrompt, systemPrompt, maxTokens, temperature);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            Map response = restTemplate.postForObject(apiUrl, request, Map.class);
            return extractContentFromResponse(response);

        } catch (Exception e) {
            log.error("Error calling LLM API", e);
            return "API 호출 중 오류가 발생했습니다.";
        }
    }

    public String callLlmApi(String userPrompt, String systemPrompt) {
        return callLlmApi(userPrompt, systemPrompt, 1000, 0.7);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private Map<String, Object> createRequestBody(String userPrompt, String systemPrompt, int maxTokens, double temperature) {
        return Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "max_tokens", maxTokens,
            "temperature", temperature
        );
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

        log.error("Failed to get response from LLM API: {}", response);
        return "응답 생성 중 오류가 발생했습니다.";
    }
} 