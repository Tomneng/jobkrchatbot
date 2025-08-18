package chatservice.infrastructure.client;

import chatservice.infrastructure.client.dto.LlmRequest;
import chatservice.infrastructure.client.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final RestTemplate restTemplate;
    
    @Value("${llm.service.url:http://localhost:8082}")
    private String llmServiceUrl;

    public LlmResponse generateResponse(LlmRequest request) {
        try {
            String url = llmServiceUrl + "/api/llm/generate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<LlmRequest> entity = new HttpEntity<>(request, headers);
            
            log.info("Calling LLM service at: {} for chat room: {}", url, request.getChatRoomId());
            
            ResponseEntity<LlmResponse> responseEntity = restTemplate.postForEntity(
                    url, entity, LlmResponse.class);
            
            LlmResponse response = responseEntity.getBody();
            
            if (response != null && response.getError() == null) {
                log.info("Successfully received LLM response for request: {}", request.getRequestId());
            } else {
                log.error("LLM service returned error: {}", response != null ? response.getError() : "null response");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error calling LLM service for request: {}", request.getRequestId(), e);
            
            return LlmResponse.builder()
                    .error("LLM 서비스 호출 중 오류가 발생했습니다: " + e.getMessage())
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
        }
    }
} 