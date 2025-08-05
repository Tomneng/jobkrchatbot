package backend.jobkrchatbot.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // 공통 에러
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(405, "C002", "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(500, "C003", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(400, "C004", "잘못된 타입의 값입니다."),
    HANDLE_ACCESS_DENIED(403, "C005", "접근이 거부되었습니다."),
    
    // 학습 관련 에러
    LEARNING_CONTENT_NOT_FOUND(404, "L001", "학습 콘텐츠를 찾을 수 없습니다."),
    INVALID_LEARNING_LEVEL(400, "L002", "잘못된 학습 레벨입니다."),
    LEARNING_PROGRESS_NOT_FOUND(404, "L003", "학습 진행 상황을 찾을 수 없습니다."),
    
    // 챗봇 관련 에러
    CHATBOT_SERVICE_UNAVAILABLE(503, "B001", "챗봇 서비스를 사용할 수 없습니다."),
    CHATBOT_RESPONSE_ERROR(500, "B002", "챗봇 응답 생성 중 오류가 발생했습니다."),
    INVALID_CHAT_REQUEST(400, "B003", "잘못된 채팅 요청입니다."),
    
    // 사용자 관련 에러
    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다."),
    USER_PROFILE_NOT_FOUND(404, "U002", "사용자 프로필을 찾을 수 없습니다."),
    INVALID_USER_PREFERENCE(400, "U003", "잘못된 사용자 선호도입니다.");
    
    private final int status;
    private final String code;
    private final String message;
} 