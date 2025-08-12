package backend.jobkrchatbot.chatservice.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartChatRequest {
    private String userId;
    private String careerSummary;
    private String jobRole;
    private List<String> technicalSkills;
    private String experience;
    private String mbtiType;
} 