package backend.jobkrchatbot.chatservice.domain.model;

public enum MessageType {
    USER("사용자"),
    ASSISTANT("어시스턴트"),
    SYSTEM("시스템");
    
    private final String description;
    
    MessageType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 