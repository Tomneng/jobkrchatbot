package backend.jobkrchatbot.application.port;

public interface LlmPort {
    String generateResponse(String prompt, String systemMessage);
} 