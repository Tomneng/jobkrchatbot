package backend.jobkrchatbot.infrastructure.external.llm;

import backend.jobkrchatbot.domain.model.LlmPrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatGptClient implements LlmClient {

    private final HttpLlmService httpLlmService;

    @Override
    public String generateResponse(String prompt, String systemMessage) {
        return httpLlmService.callLlmApi(prompt, systemMessage);
    }

} 