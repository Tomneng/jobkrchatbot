package backend.jobkrchatbot.application.port;

public interface LlmPort {
    String generateInterviewQuestions(String resumeInfo);
    String generateLearningPath(String resumeInfo);
} 