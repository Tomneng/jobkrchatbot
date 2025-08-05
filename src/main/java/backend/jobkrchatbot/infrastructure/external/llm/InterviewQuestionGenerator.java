package backend.jobkrchatbot.infrastructure.external.llm;

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
public class InterviewQuestionGenerator {

    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    public String generateInterviewQuestions(String resumeInfo) {
        try {
            String systemPrompt = """
                당신은 IT 업계에서 10년 이상 경력의 시니어 개발자이자 면접관입니다.
                주어진 이력서 정보를 바탕으로 실제 면접에서 나올 법한 심층적인 기술 면접 질문 5개를 생성해주세요.
                
                질문 생성 시 다음을 고려해주세요:
                1. 경력과 직무에 맞는 적절한 난이도
                2. 기술 스택에 대한 깊이 있는 이해도 확인
                3. 실제 프로젝트 경험을 바탕으로 한 구체적인 질문
                4. 문제 해결 능력과 시스템 설계 능력 평가
                5. 최신 기술 트렌드와의 연관성
                
                응답 형식:
                ## 기술 면접 질문
                
                ### 1. [질문 제목]
                질문 내용...
                
                ### 2. [질문 제목]
                질문 내용...
                
                (이하 5개까지)
                """;

            return callOpenAI(resumeInfo, systemPrompt);

        } catch (Exception e) {
            log.error("Error generating interview questions", e);
            return "면접 질문 생성 중 오류가 발생했습니다.";
        }
    }

    private String callOpenAI(String userPrompt, String systemPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 2000,
                "temperature", 0.7
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    return (String) message.get("content");
                }
            }

            log.error("Failed to get response from OpenAI API: {}", response);
            return "응답 생성 중 오류가 발생했습니다.";

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            return "API 호출 중 오류가 발생했습니다.";
        }
    }
} 