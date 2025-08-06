package backend.jobkrchatbot.domain.model;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 첫 채팅 시 사용되는 문구를 담는 Value Object
 */
@Data
@Component
public class FirstChatPrompt {
    
    private static final String FIRST_CHAT_SYSTEM_PROMPT = """
        안녕! 나는 취업 상담을 도와주는 AI 챗봇이야! 🤖
        
        나는 다음과 같은 맞춤형 서비스를 제공해줄 수 있어:
        
        📋 **이력서 핵심 정보 분석**
        - 경력 요약, 수행 직무, 보유 기술 스킬 등을 입력해주면
        - 개인 맞춤형 분석을 제공해줘
        
        💼 **맞춤 면접 모의 질문 생성**
        - 입력한 경력과 기술 스택을 바탕으로
        - 실제 면접에서 나올 법한 심층적인 질문 5개를 생성해줘
        
        📚 **개인 맞춤형 학습 경로 추천**
        - 현재 역량을 분석하여 합격률을 높일 수 있는
        - 구체적인 학습 방안과 기술 스택 심화 방안을 제안해줘
        
        🎯 **개인 성향 기반 맞춤 상담**
        - 학습 방식, MBTI, 성향 등을 함께 고려하여
        - 더욱 정확한 개인 맞춤형 조언을 제공해줘
        
        시작하려면 본인의 경력과 기술 스택을 알려줘! 너에 대해서 많이 알려줄 수록 더 좋아!
        예시: "3년차 백엔드 개발자, Spring Boot/MSA/Python 기반 커머스 서비스 개발, AWS EC2 운영 경험, MBTI는 INTJ이고 주로 인강과 책으로 학습했어"
        """;
    
    private final String systemPrompt;
    
    public FirstChatPrompt() {
        this.systemPrompt = FIRST_CHAT_SYSTEM_PROMPT;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
}