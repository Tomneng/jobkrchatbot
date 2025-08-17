package backend.jobkrchatbot.llmservice.service;

import backend.jobkrchatbot.common.dto.LlmRequest;
import backend.jobkrchatbot.common.dto.LlmResponse;
import backend.jobkrchatbot.llmservice.infrastructure.GptClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final GptClient gptClient;
    private final KafkaTemplate<String, LlmResponse> kafkaTemplate;
    
    private static final String LLM_RESPONSE_TOPIC = "llm.responses";

    @KafkaListener(topics = "user-messages", groupId = "llm-service")
    public void handleLlmRequest(String message) {
        try {
            log.info("Processing user message: {}", message);
            
            // JSON 메시지를 파싱하여 필요한 정보 추출
            // 간단한 예시 - 실제로는 JSON 파서 사용 권장
            String[] parts = message.replaceAll("[{}\"]", "").split(",");
            String chatRoomId = "";
            String userId = "";
            String userMessage = "";
            
            for (String part : parts) {
                if (part.contains("chatRoomId:")) {
                    chatRoomId = part.split(":")[1];
                } else if (part.contains("userId:")) {
                    userId = part.split(":")[1];
                } else if (part.contains("message:")) {
                    userMessage = part.split(":")[1];
                }
            }
            
            // 구직자 맞춤형 프롬프트 생성
            String systemPrompt = createJobSeekerPrompt();
            
            // GPT API 호출 - 사용자 메시지와 시스템 프롬프트 조합
            String response = gptClient.generateResponse(userMessage, systemPrompt);
            
            // 응답 생성
            LlmResponse llmResponse = LlmResponse.builder()
                    .response(response)
                    .chatRoomId(chatRoomId)
                    .userId(userId)
                    .requestId(java.util.UUID.randomUUID().toString())
                    .build();
            
            // 응답을 Kafka로 전송
            kafkaTemplate.send(LLM_RESPONSE_TOPIC, chatRoomId, llmResponse);
            log.info("LLM response sent successfully for chat room: {}", chatRoomId);
            
        } catch (Exception e) {
            log.error("Error processing user message: {}", message, e);
        }
    }

    public LlmResponse generateResponse(LlmRequest request) {
        try {
            // 구직자 맞춤형 프롬프트 생성
            String systemPrompt = createJobSeekerPrompt();
            
            // GPT API 호출 - 사용자 메시지와 시스템 프롬프트 조합
            String response = gptClient.generateResponse(request.getUserMessage(), systemPrompt);
            
            return LlmResponse.builder()
                    .response(response)
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error generating response", e);
            return LlmResponse.builder()
                    .error("응답 생성 중 오류가 발생했습니다.")
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
        }
    }
    
    /**
     * 구직자를 위한 맞춤형 시스템 프롬프트를 생성합니다.
     * 이력서 기반 면접 질문 생성과 학습 경로 제안을 위한 프롬프트입니다.
     */
    private String createJobSeekerPrompt() {
        return """
            당신은 구직자의 합격률을 높이기 위한 전문적인 AI 면접 코치입니다.
            
            주요 역할:
            1. 맞춤형 면접 모의질문 생성
            2. 자기 개발 및 학습 경로 제안
            3. 구체적이고 실용적인 조언 제공
            
            응답 형식:
            사용자가 이력서 정보(경력, 직무, 기술 스킬)를 입력하면 다음 두 가지를 제공하세요:
            
            📝 맞춤형 면접 질문 (5개)
            - 입력된 경력과 기술 스킬에 기반한 실제 면접에서 나올 법한 질문
            - 기술적 깊이와 경험을 평가하는 질문
            - 구체적이고 실용적인 상황 기반 질문
            
            🚀 자기 개발 학습 경로
            - 기술 스택 심화 방안
            - 관련 프로젝트 경험 쌓기 방법
            - 커뮤니케이션 스킬 강화 방안
            - 합격률 향상을 위한 구체적인 액션 플랜
            
            응답은 한국어로 작성하고, 구체적이고 실용적인 내용으로 구성해주세요.
            """;
    }
} 