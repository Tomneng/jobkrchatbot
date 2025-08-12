package backend.jobkrchatbot.domain.service;

import backend.jobkrchatbot.application.port.LlmPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmPort llmPort;

<<<<<<< HEAD
    public String generateChatResponse(String userMessage, String systemMessage) {
=======
    /**
     * RAG 방식: 사용자 메시지와 시스템 컨텍스트를 조합하여 LLM 응답 생성
     */
    public String generateChatResponse(String userMessage, String contextWithHistory) {
        // 사용자 메시지를 컨텍스트에 추가하여 최종 프롬프트 생성
        String finalPrompt = contextWithHistory + "\n\n사용자: " + userMessage + "\n\n답변:";
        return llmPort.generateResponse(finalPrompt, "");
    }
    
    /**
     * 기존 방식: 사용자 메시지와 시스템 메시지를 분리하여 전달
     */
    public String generateChatResponseLegacy(String userMessage, String systemMessage) {
>>>>>>> c03c19066653ad63ce423430e01af23d6cd7f95c
        return llmPort.generateResponse(userMessage, systemMessage);
    }
} 