package backend.jobkrchatbot.infrastructure.external.llm;

public interface LlmClient {
    String generateResponse(String prompt, String systemMessage);
} 