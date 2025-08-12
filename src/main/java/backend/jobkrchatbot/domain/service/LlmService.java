package backend.jobkrchatbot.domain.service;

import backend.jobkrchatbot.application.port.LlmPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmService {

    private final LlmPort llmPort;

    public String generateChatResponse(String userMessage, String systemMessage) {
        return llmPort.generateResponse(userMessage, systemMessage);
    }
} 