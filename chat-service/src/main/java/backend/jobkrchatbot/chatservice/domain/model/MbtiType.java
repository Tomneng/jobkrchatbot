package backend.jobkrchatbot.chatservice.domain.model;

public enum MbtiType {
    // 분석가형
    INTJ("건축가", "전략적 사고, 독립적, 분석적"),
    INTP("논리술사", "논리적 사고, 창의적, 객관적"),
    ENTJ("통솔자", "리더십, 결단력, 효율성"),
    ENTP("변론가", "혁신적, 적응력, 창의성"),
    
    // 외교관형
    INFJ("옹호자", "공감능력, 이상주의, 창의성"),
    INFP("중재자", "창의성, 공감능력, 유연성"),
    ENFJ("선도자", "공감능력, 리더십, 협력"),
    ENFP("활동가", "열정, 창의성, 적응력"),
    
    // 관리자형
    ISTJ("현실주의자", "신뢰성, 책임감, 실용성"),
    ISFJ("수호자", "책임감, 공감능력, 실용성"),
    ESTJ("경영자", "리더십, 조직력, 효율성"),
    ESFJ("집정관", "협력, 책임감, 실용성"),
    
    // 탐험가형
    ISTP("만능재주꾼", "적응력, 실용성, 독립성"),
    ISFP("모험가", "창의성, 공감능력, 실용성"),
    ESTP("사업가", "적응력, 실용성, 독립성"),
    ESFP("연예인", "열정, 적응력, 협력");
    
    private final String title;
    private final String description;
    
    MbtiType(String title, String description) {
        this.title = title;
        this.description = description;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static MbtiType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        
        try {
            return MbtiType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
} 