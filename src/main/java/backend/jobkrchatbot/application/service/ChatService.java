package backend.jobkrchatbot.application.service;

import backend.jobkrchatbot.application.dto.ChatRequest;
import backend.jobkrchatbot.application.dto.ChatResponse;
import backend.jobkrchatbot.application.port.LlmPort;
import backend.jobkrchatbot.domain.entity.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final LlmPort llmPort;

    public ChatResponse generateChatResponse(ChatRequest request) {
        try {
            String systemMessage = "당신은 취업 상담을 도와주는 전문가입니다. 사용자의 질문에 친절하고 전문적으로 답변해주세요.";
            // 여기서는 일반 채팅이므로 기존 방식 유지
            return ChatResponse.builder()
                    .message("일반 채팅 기능은 현재 개발 중입니다.")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error generating chat response", e);
            return ChatResponse.builder()
                    .message("죄송합니다. 응답을 생성하는 중 오류가 발생했습니다.")
                    .build();
        }
    }
} 