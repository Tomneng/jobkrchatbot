package chatservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// API Gateway에서 CORS를 처리하므로 Chat Service의 CORS 설정은 비활성화
// @Configuration
public class CorsConfig implements WebMvcConfigurer {

    // @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                    "http://localhost:5500",
                    "http://127.0.0.1:5500",
                    "http://localhost:3000",
                    "http://127.0.0.1:3000"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
} 