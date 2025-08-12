package backend.jobkrchatbot.llmservice.service;

import backend.jobkrchatbot.common.dto.LlmRequest;
import backend.jobkrchatbot.common.dto.LlmResponse;
import backend.jobkrchatbot.llmservice.infrastructure.ClaudeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final ClaudeClient claudeClient;
    private final KafkaTemplate<String, LlmResponse> kafkaTemplate;
    
    private static final String LLM_RESPONSE_TOPIC = "llm.responses";

    @KafkaListener(topics = "llm.requests", groupId = "llm-service")
    public void handleLlmRequest(LlmRequest request) {
        try {
            log.info("Processing LLM request: {}", request.getRequestId());
            
            // Claude API 호출
            String response = claudeClient.generateResponse(request.getUserMessage(), request.getSystemMessage());
            
            // 응답 생성
            LlmResponse llmResponse = LlmResponse.builder()
                    .response(response)
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
            
            // 응답을 Kafka로 전송
            kafkaTemplate.send(LLM_RESPONSE_TOPIC, request.getRequestId(), llmResponse);
            log.info("LLM response sent successfully: {}", request.getRequestId());
            
        } catch (Exception e) {
            log.error("Error processing LLM request: {}", request.getRequestId(), e);
            
            // 에러 응답 전송
            LlmResponse errorResponse = LlmResponse.builder()
                    .error("응답 생성 중 오류가 발생했습니다.")
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
            
            kafkaTemplate.send(LLM_RESPONSE_TOPIC, request.getRequestId(), errorResponse);
        }
    }

    public LlmResponse generateResponse(LlmRequest request) {
        try {
            String response = claudeClient.generateResponse(request.getUserMessage(), request.getSystemMessage());
            
            return LlmResponse.builder()
                    .response(response)
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error generating response", e);
            return LlmResponse.builder()
                    .error("응답 생성 중 오류가 발생했습니다.")
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
        }
    }
} 