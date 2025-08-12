package backend.jobkrchatbot.chatservice.domain.model;

public enum ChatStatus {
    ACTIVE("활성"),
    ENDED("종료"),
    PAUSED("일시정지");
    
    private final String description;
    
    ChatStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 