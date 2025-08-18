package chatservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    // RestTemplate은 더 이상 사용하지 않음 - WebClient로 대체됨
    // @Bean
    // public RestTemplate restTemplate(RestTemplateBuilder builder) {
    //     return builder
    //             .setConnectTimeout(Duration.ofSeconds(10))
    //             .setReadTimeout(Duration.ofSeconds(30))
    //             .build();
    // }
} 