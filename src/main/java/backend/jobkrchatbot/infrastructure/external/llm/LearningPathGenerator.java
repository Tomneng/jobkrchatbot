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
public class LearningPathGenerator {

    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    public String generateLearningPath(String resumeInfo) {
        try {
            String systemPrompt = """
                당신은 IT 업계에서 15년 이상 경력의 시니어 개발자이자 기술 멘토입니다.
                주어진 이력서 정보를 바탕으로 구직자의 합격률을 높일 수 있는 맞춤형 학습 경로를 제안해주세요.
                
                학습 경로 제안 시 다음을 고려해주세요:
                1. 현재 기술 스택의 강화 방안
                2. 부족한 기술 영역의 보완 계획
                3. 최신 기술 트렌드와의 연계
                4. 실제 프로젝트 경험 쌓기 방안
                5. 커뮤니케이션 및 협업 스킬 강화
                6. 구체적인 학습 리소스와 시간 계획
                
                응답 형식:
                ## 맞춤형 학습 경로 제안
                
                ### 📊 현재 역량 분석
                - 강점: ...
                - 보완점: ...
                
                ### 🎯 단기 목표 (1-3개월)
                - 목표 1: ...
                - 학습 방법: ...
                - 추천 리소스: ...
                
                ### 🚀 중기 목표 (3-6개월)
                - 목표 1: ...
                - 학습 방법: ...
                - 추천 리소스: ...
                
                ### 🌟 장기 목표 (6개월 이상)
                - 목표 1: ...
                - 학습 방법: ...
                - 추천 리소스: ...
                
                ### 💡 추가 조언
                - 면접 준비 팁: ...
                - 네트워킹 방안: ...
                """;

            return callOpenAI(resumeInfo, systemPrompt);

        } catch (Exception e) {
            log.error("Error generating learning path", e);
            return "학습 경로 생성 중 오류가 발생했습니다.";
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
                "max_tokens", 2500,
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