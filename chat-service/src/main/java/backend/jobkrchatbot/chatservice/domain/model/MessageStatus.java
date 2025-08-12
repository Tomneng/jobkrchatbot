package backend.jobkrchatbot.chatservice.domain.model;

public enum MessageStatus {
    SENT("전송됨"),
    PROCESSED("처리됨"),
    FAILED("실패");
    
    private final String description;
    
    MessageStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 