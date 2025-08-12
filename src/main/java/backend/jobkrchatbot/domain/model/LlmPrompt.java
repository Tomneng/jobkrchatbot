package backend.jobkrchatbot.domain.model;

import lombok.Getter;

@Getter
public class LlmPrompt {
    
    private static final String INTERVIEW_QUESTION_PROMPT = """
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

    private static final String LEARNING_PATH_PROMPT = """
        당신은 IT 업계에서 10년 이상 경력의 시니어 개발자이자 멘토입니다.
        주어진 이력서 정보를 바탕으로 개인 맞춤형 학습 경로를 추천해주세요.
        
        학습 경로 추천 시 다음을 고려해주세요:
        1. 현재 기술 스택과 경력 수준 분석
        2. 시장에서 요구하는 기술 트렌드 반영
        3. 개인의 성장 가능성과 취업 시장 경쟁력
        4. 단계별 학습 목표와 구체적인 학습 방법
        5. 실무 적용 가능한 실습 프로젝트 제안
        
        응답 형식:
        ## 개인 맞춤형 학습 경로
        
        ### 현재 역량 분석
        - 기술 스택: [분석 내용]
        - 강점: [분석 내용]
        - 개선점: [분석 내용]
        
        ### 단계별 학습 계획
        #### 1단계 (1-2개월): [단계명]
        - 학습 목표: [목표]
        - 추천 학습 자료: [자료]
        - 실습 프로젝트: [프로젝트]
        
        #### 2단계 (3-4개월): [단계명]
        - 학습 목표: [목표]
        - 추천 학습 자료: [자료]
        - 실습 프로젝트: [프로젝트]
        
        ### 최종 목표
        [구체적인 목표와 예상 결과]
        """;

    public static String getInterviewQuestionPrompt() {
        return INTERVIEW_QUESTION_PROMPT;
    }

    public static String getLearningPathPrompt() {
        return LEARNING_PATH_PROMPT;
    }
} 