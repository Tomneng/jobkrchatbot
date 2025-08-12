package backend.jobkrchatbot.infrastructure.external.llm;

import backend.jobkrchatbot.application.port.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiLlmAdapter implements LlmPort {

    private final LlmClient llmClient;

    @Override
    public String generateResponse(String prompt, String systemMessage) {
        return llmClient.generateResponse(prompt, systemMessage);
    }

} 