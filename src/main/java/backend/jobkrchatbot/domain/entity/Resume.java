package backend.jobkrchatbot.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {
    private String careerSummary;
    private String jobTitle;
    private List<String> skills;
    private String experience;
    
    public boolean isValid() {
        return careerSummary != null && !careerSummary.trim().isEmpty() &&
               jobTitle != null && !jobTitle.trim().isEmpty() &&
               skills != null && !skills.isEmpty();
    }
    
    public String getFormattedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("경력: ").append(careerSummary).append("\n");
        sb.append("직무: ").append(jobTitle).append("\n");
        sb.append("기술 스킬: ").append(String.join(", ", skills)).append("\n");
        if (experience != null && !experience.trim().isEmpty()) {
            sb.append("주요 경험: ").append(experience);
        }
        return sb.toString();
    }
} 