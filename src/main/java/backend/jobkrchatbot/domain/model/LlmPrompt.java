package backend.jobkrchatbot.domain.model;

import lombok.Getter;

@Getter
public class LlmPrompt {
    
<<<<<<< HEAD
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
=======
    private static final String UNIFIED_PROMPT = """
        당신은 IT 업계에서 10년 이상 경력의 시니어 개발자이자 멘토입니다.
        사용자의 이력서 정보를 바탕으로 맞춤형 조언을 제공해주세요.
        
        ## 역할 및 전문 분야
        1. **기술 면접 준비**: 실제 면접에서 나올 법한 심층적인 기술 면접 질문 생성
        2. **학습 경로 추천**: 개인 맞춤형 학습 경로 및 성장 전략 제시
        3. **일반 취업 상담**: 이력서 분석, 취업 전략, 경력 개발 조언
        
        ## 면접 질문 생성 시 고려사항
        - 경력과 직무에 맞는 적절한 난이도
        - 기술 스택에 대한 깊이 있는 이해도 확인
        - 실제 프로젝트 경험을 바탕으로 한 구체적인 질문
        - 문제 해결 능력과 시스템 설계 능력 평가
        - 최신 기술 트렌드와의 연관성
        
        ## 학습 경로 추천 시 고려사항
        - 현재 기술 스택과 경력 수준 분석
        - 시장에서 요구하는 기술 트렌드 반영
        - 개인의 성장 가능성과 취업 시장 경쟁력
        - 단계별 학습 목표와 구체적인 학습 방법
        - 실무 적용 가능한 실습 프로젝트 제안
        
        ## 응답 형식 가이드
        
        ### 면접 질문 요청 시:
>>>>>>> c03c19066653ad63ce423430e01af23d6cd7f95c
        ## 기술 면접 질문
        
        ### 1. [질문 제목]
        질문 내용...
        
        ### 2. [질문 제목]
        질문 내용...
        
        (이하 5개까지)
<<<<<<< HEAD
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
=======
        
        ### 학습 경로 요청 시:
>>>>>>> c03c19066653ad63ce423430e01af23d6cd7f95c
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
<<<<<<< HEAD
        """;

    public static String getInterviewQuestionPrompt() {
        return INTERVIEW_QUESTION_PROMPT;
    }

    public static String getLearningPathPrompt() {
        return LEARNING_PATH_PROMPT;
=======
        
        ### 일반 상담 요청 시:
        이력서를 바탕으로 한 구체적이고 실용적인 조언을 제공하세요.
        """;

    /**
     * 사용자 메시지의 의도를 분석하여 학습/면접 관련 요청인지 판단
     */
    public static boolean isLearningOrInterviewRequest(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        
        // 학습 관련 키워드
        String[] learningKeywords = {
            "학습", "공부", "스터디", "교육", "강의", "코스", "과정", "로드맵", "경로",
            "성장", "발전", "개발", "프로그래밍", "코딩", "기술", "스킬", "역량",
            "추천", "방법", "전략", "계획", "목표", "단계", "단계별", "학습방법"
        };
        
        // 면접 관련 키워드
        String[] interviewKeywords = {
            "면접", "질문", "인터뷰", "기술면접", "코딩테스트", "알고리즘",
            "시험", "평가", "테스트", "문제", "풀이", "해결", "답변",
            "준비", "팁", "조언", "가이드", "체크리스트"
        };
        
        // 키워드 매칭 확인
        for (String keyword : learningKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        
        for (String keyword : interviewKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 통합된 프롬프트 반환
     */
    public static String getUnifiedPrompt() {
        return UNIFIED_PROMPT;
    }
    
    /**
     * 의도에 따른 시스템 메시지 생성
     */
    public static String generateSystemMessage(String userMessage, boolean isFirstMessage) {
        if (isFirstMessage) {
            return UNIFIED_PROMPT + "\n\n이력서 정보를 바탕으로 맞춤형 조언을 제공해주세요.";
        }
        
        if (isLearningOrInterviewRequest(userMessage)) {
            return UNIFIED_PROMPT + "\n\n사용자의 요청에 따라 구체적이고 실용적인 답변을 제공해주세요.";
        } else {
            return "당신은 취업 상담을 도와주는 전문가입니다. 이전 대화 내용을 참고하여 일관성 있는 답변을 제공해주세요.";
        }
>>>>>>> c03c19066653ad63ce423430e01af23d6cd7f95c
    }
} 