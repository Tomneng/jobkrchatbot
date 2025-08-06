package backend.jobkrchatbot.infrastructure.external.llm;

import backend.jobkrchatbot.application.port.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiLlmAdapter implements LlmPort {

    private final InterviewQuestionGenerator interviewQuestionGenerator;
    private final LearningPathGenerator learningPathGenerator;
    private final ChatGptClient chatGptClient;

    @Override
    public String generateResponse(String prompt, String systemMessage) {
        return chatGptClient.generateResponse(prompt, systemMessage);
    }

    @Override
    public String generateInterviewQuestions(String resumeInfo) {
        return interviewQuestionGenerator.generateInterviewQuestions(resumeInfo);
    }

    @Override
    public String generateLearningPath(String resumeInfo) {
        return learningPathGenerator.generateLearningPath(resumeInfo);
    }
} 