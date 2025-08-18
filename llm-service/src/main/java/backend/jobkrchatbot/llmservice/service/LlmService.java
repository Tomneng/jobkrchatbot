package backend.jobkrchatbot.llmservice.service;

import backend.jobkrchatbot.llmservice.dto.LlmRequest;
import backend.jobkrchatbot.llmservice.dto.LlmResponse;
import backend.jobkrchatbot.llmservice.infrastructure.GptClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final GptClient gptClient;

    public LlmResponse generateResponse(LlmRequest request) {
        try {
            log.info("Generating response for chat room: {}, user: {}", 
                    request.getChatRoomId(), request.getUserId());
            
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
            log.error("Error generating response for request: {}", request.getRequestId(), e);
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