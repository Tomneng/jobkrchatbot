package backend.jobkrchatbot.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

// CORS 설정을 application.yml의 globalcors로 이동하여 중복 방지
// @Configuration
public class CorsConfig {

    // @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // 허용할 Origin 설정 (개발 환경)
        List<String> allowedOrigins = Arrays.asList(
            "http://localhost:5500",
            "http://127.0.0.1:5500",
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        );
        corsConfig.setAllowedOriginPatterns(allowedOrigins);
        
        // 허용할 HTTP 메서드
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 허용할 헤더
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Origin", "Content-Type", "Accept", "Authorization", 
            "X-Requested-With", "Cache-Control", "X-User-Id"
        ));
        
        // 인증 정보 허용
        corsConfig.setAllowCredentials(true);
        
        // Preflight 요청 캐시 시간
        corsConfig.setMaxAge(3600L);
        
        // 노출할 헤더
        corsConfig.setExposedHeaders(Arrays.asList("X-User-Id", "X-Total-Count"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
