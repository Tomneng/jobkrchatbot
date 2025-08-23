package backend.jobkrchatbot.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
            .then(Mono.fromRunnable(() -> {
                ServerHttpResponse response = exchange.getResponse();
                HttpHeaders headers = response.getHeaders();
                
                // 보안 헤더 추가
                headers.add("X-Content-Type-Options", "nosniff");
                headers.add("X-Frame-Options", "DENY");
                headers.add("X-XSS-Protection", "1; mode=block");
                headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
                headers.add("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
                
                // CORS 헤더 (필요시)
                if (!headers.containsKey("Access-Control-Allow-Origin")) {
                    headers.add("Access-Control-Allow-Origin", "*");
                }
                if (!headers.containsKey("Access-Control-Allow-Methods")) {
                    headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                }
                if (!headers.containsKey("Access-Control-Allow-Headers")) {
                    headers.add("Access-Control-Allow-Headers", "*");
                }
            }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
