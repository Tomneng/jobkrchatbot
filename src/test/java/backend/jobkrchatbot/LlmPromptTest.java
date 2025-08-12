package backend.jobkrchatbot;

import backend.jobkrchatbot.domain.model.LlmPrompt;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LlmPromptTest {

    @Test
    void testLearningRequestDetection() {
        // 학습 관련 요청 테스트
        assertTrue(LlmPrompt.isLearningOrInterviewRequest("학습 방법을 알려주세요"));
        assertTrue(LlmPrompt.isLearningOrInterviewRequest("공부 계획을 세우고 싶어요"));
        assertTrue(LlmPrompt.isLearningOrInterviewRequest("개발자 로드맵 추천해주세요"));
        assertTrue(LlmPrompt.isLearningOrInterviewRequest("기술 스택 학습 전략"));
        assertTrue(LlmPrompt.isLearningOrInterviewRequest("단계별 성장 방법"));
    }

    @Test
    void testInterviewRequestDetection() {
        // 면접 관련 요청 테스트
        assertTrue(LlmPrompt.isLearningOrInterviewRequest("면접 질문 알려주세요"));
        assertTrue(LlmPrompt.isLearningOrInterviewRequest("기술면접 준비 방법"));
        assertTrue(LlmPrompt.isLearningOrInterviewRequest("코딩테스트 팁"));
        assertTrue(LlmPrompt.isLearningOrInterviewRequest("알고리즘 문제 풀이"));
        assertTrue(LlmPrompt.isLearningOrInterviewRequest("면접 체크리스트"));
    }

    @Test
    void testGeneralRequestDetection() {
        // 일반 요청 테스트 (false 반환)
        assertFalse(LlmPrompt.isLearningOrInterviewRequest("안녕하세요"));
        assertFalse(LlmPrompt.isLearningOrInterviewRequest("이력서를 봐주세요"));
        assertFalse(LlmPrompt.isLearningOrInterviewRequest("취업 조언"));
        assertFalse(LlmPrompt.isLearningOrInterviewRequest("자기소개서"));
    }

    @Test
    void testSystemMessageGeneration() {
        // 첫 메시지 테스트
        String firstMessage = LlmPrompt.generateSystemMessage("학습 방법 알려주세요", true);
        assertTrue(firstMessage.contains("IT 업계에서 10년 이상 경력"));
        assertTrue(firstMessage.contains("이력서 정보를 바탕으로 맞춤형 조언"));
        
        // 학습/면접 관련 요청 테스트
        String learningMessage = LlmPrompt.generateSystemMessage("면접 질문 알려주세요", false);
        assertTrue(learningMessage.contains("구체적이고 실용적인 답변"));
        
        // 일반 요청 테스트
        String generalMessage = LlmPrompt.generateSystemMessage("안녕하세요", false);
        assertTrue(generalMessage.contains("취업 상담을 도와주는 전문가"));
    }
} 