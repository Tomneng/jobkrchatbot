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

    @KafkaListener(topics = "user-messages", groupId = "llm-service")
    public void handleLlmRequest(String message) {
        try {
            log.info("Processing user message: {}", message);
            
            // JSON 메시지를 파싱하여 필요한 정보 추출
            // 간단한 예시 - 실제로는 JSON 파서 사용 권장
            String[] parts = message.replaceAll("[{}\"]", "").split(",");
            String chatRoomId = "";
            String userId = "";
            String userMessage = "";
            
            for (String part : parts) {
                if (part.contains("chatRoomId:")) {
                    chatRoomId = part.split(":")[1];
                } else if (part.contains("userId:")) {
                    userId = part.split(":")[1];
                } else if (part.contains("message:")) {
                    userMessage = part.split(":")[1];
                }
            }
            
            // Claude API 호출
            String response = claudeClient.generateResponse(userMessage, "You are a helpful AI assistant.");
            
            // 응답 생성
            LlmResponse llmResponse = LlmResponse.builder()
                    .response(response)
                    .chatRoomId(chatRoomId)
                    .userId(userId)
                    .requestId(java.util.UUID.randomUUID().toString())
                    .build();
            
            // 응답을 Kafka로 전송
            kafkaTemplate.send(LLM_RESPONSE_TOPIC, chatRoomId, llmResponse);
            log.info("LLM response sent successfully for chat room: {}", chatRoomId);
            
        } catch (Exception e) {
            log.error("Error processing user message: {}", message, e);
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