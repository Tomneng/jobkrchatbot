package backend.jobkrchatbot.chatservice.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResumeInfo {
    
    private final String careerSummary;
    private final String jobRole;
    private final List<String> technicalSkills;
    private final String experience;
    
    public ResumeInfo(String careerSummary, String jobRole, List<String> technicalSkills, String experience) {
        if (careerSummary == null || careerSummary.trim().isEmpty()) {
            throw new IllegalArgumentException("경력 요약은 필수입니다.");
        }
        if (jobRole == null || jobRole.trim().isEmpty()) {
            throw new IllegalArgumentException("직무는 필수입니다.");
        }
        if (technicalSkills == null || technicalSkills.isEmpty()) {
            throw new IllegalArgumentException("기술 스킬은 필수입니다.");
        }
        if (experience == null || experience.trim().isEmpty()) {
            throw new IllegalArgumentException("주요 경험은 필수입니다.");
        }
        
        this.careerSummary = careerSummary.trim();
        this.jobRole = jobRole.trim();
        this.technicalSkills = new ArrayList<>(technicalSkills);
        this.experience = experience.trim();
    }
    
    // 도메인 메서드들
    public String getFormattedSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("경력: ").append(careerSummary).append("\n");
        summary.append("직무: ").append(jobRole).append("\n");
        summary.append("기술 스킬: ").append(String.join(", ", technicalSkills)).append("\n");
        summary.append("주요 경험: ").append(experience);
        return summary.toString();
    }
    
    public boolean hasTechnicalSkills() {
        return !technicalSkills.isEmpty();
    }
    
    public boolean isComplete() {
        return true; // 생성자에서 이미 검증됨
    }
    
    // Getter 메서드들
    public String getCareerSummary() { return careerSummary; }
    public String getJobRole() { return jobRole; }
    public List<String> getTechnicalSkills() { return Collections.unmodifiableList(technicalSkills); }
    public String getExperience() { return experience; }
} 