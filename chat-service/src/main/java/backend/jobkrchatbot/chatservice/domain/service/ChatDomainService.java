package backend.jobkrchatbot.chatservice.domain.service;

import backend.jobkrchatbot.chatservice.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatDomainService {
    
    public ChatRoom createChatRoom(UserId userId, ResumeInfo resumeInfo, MbtiType mbtiType) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        
        if (resumeInfo == null || !resumeInfo.isComplete()) {
            throw new IllegalArgumentException("완전한 이력서 정보가 필요합니다.");
        }
        
        if (mbtiType == null) {
            throw new IllegalArgumentException("MBTI 타입은 필수입니다.");
        }
        
        return ChatRoom.builder()
                .userId(userId)
                .resumeInfo(resumeInfo)
                .mbtiType(mbtiType)
                .build();
    }
    
    public void validateChatSession(ChatRoom chatRoom) {
        if (chatRoom == null) {
            throw new IllegalArgumentException("채팅방이 존재하지 않습니다.");
        }
        
        if (chatRoom.getStatus() != ChatStatus.ACTIVE) {
            throw new IllegalStateException("비활성 상태의 채팅방입니다.");
        }
    }
    
    public String generateWelcomeMessage(ChatRoom chatRoom) {
        ResumeInfo resumeInfo = chatRoom.getResumeInfo();
        MbtiType mbtiType = chatRoom.getMbtiType();
        
        StringBuilder welcome = new StringBuilder();
        welcome.append("안녕하세요! 🎯\n\n");
        welcome.append("저는 당신의 이력서와 MBTI를 기반으로 맞춤형 커리어 코칭을 제공하는 AI 어시스턴트입니다.\n\n");
        welcome.append("📋 **이력서 요약**\n");
        welcome.append(resumeInfo.getFormattedSummary()).append("\n\n");
        welcome.append("🧠 **MBTI 특성**: ").append(mbtiType.getTitle()).append(" (").append(mbtiType.getDescription()).append(")\n\n");
        welcome.append("이제 당신에게 맞춤형 면접 질문과 학습 경로를 제안해드리겠습니다!\n\n");
        welcome.append("어떤 도움이 필요하신가요?\n");
        welcome.append("1️⃣ 맞춤형 면접 질문 생성\n");
        welcome.append("2️⃣ 개인 맞춤 학습 경로 제안\n");
        welcome.append("3️⃣ 이력서 개선 조언\n");
        welcome.append("4️⃣ 기타 질문");
        
        return welcome.toString();
    }
    
    public String generateMbtiBasedPrompt(String userMessage, MbtiType mbtiType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("사용자의 MBTI는 ").append(mbtiType.getTitle()).append("(").append(mbtiType.getDescription()).append(")입니다.\n\n");
        
        // MBTI별 맞춤 프롬프트 생성
        switch (mbtiType) {
            case INTJ:
            case INTP:
                prompt.append("이 사용자는 분석적이고 논리적 사고를 선호합니다. 구체적이고 체계적인 답변을 제공하세요.");
                break;
            case ENFJ:
            case ENFP:
                prompt.append("이 사용자는 감정적이고 창의적입니다. 따뜻하고 격려적인 톤으로 답변하세요.");
                break;
            case ISTJ:
            case ESTJ:
                prompt.append("이 사용자는 실용적이고 체계적입니다. 구체적이고 실현 가능한 조언을 제공하세요.");
                break;
            case ISFP:
            case ESFP:
                prompt.append("이 사용자는 유연하고 적응력이 좋습니다. 다양한 옵션과 실용적인 조언을 제공하세요.");
                break;
            default:
                prompt.append("사용자의 개성을 고려하여 맞춤형 답변을 제공하세요.");
        }
        
        prompt.append("\n\n사용자 메시지: ").append(userMessage);
        return prompt.toString();
    }
} 