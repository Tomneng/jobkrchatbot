package backend.jobkrchatbot.config;

import backend.jobkrchatbot.infrastructure.external.llm.ChatGptClient;
import backend.jobkrchatbot.infrastructure.external.llm.ClaudeClient;
import backend.jobkrchatbot.infrastructure.external.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class LlmConfig {

    @Value("${llm.provider:openai}")
    private String llmProvider;

    private final ChatGptClient chatGptClient;
    private final ClaudeClient claudeClient;

    @Bean
    public LlmClient llmClient() {
        return switch (llmProvider.toLowerCase()) {
            case "anthropic", "claude" -> claudeClient;
            case "openai", "gpt" -> chatGptClient;
            default -> chatGptClient; // 기본값은 OpenAI
        };
    }
} 