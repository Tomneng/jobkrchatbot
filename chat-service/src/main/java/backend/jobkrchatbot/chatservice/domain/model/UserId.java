package backend.jobkrchatbot.chatservice.domain.model;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class UserId {
    
    private final String value;
    
    public UserId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("UserId는 비어있을 수 없습니다.");
        }
        this.value = value;
    }
    
    @Override
    public String toString() {
        return value;
    }
} 