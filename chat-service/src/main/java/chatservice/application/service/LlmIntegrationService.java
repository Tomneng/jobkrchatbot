package chatservice.application.service;

import chatservice.domain.port.LlmService;
import chatservice.domain.port.MessagePublisher;
import chatservice.infrastructure.client.dto.LlmRequest;
import chatservice.application.dto.SendMessageRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmIntegrationService {
    
    private final MessagePublisher messagePublisher;
    
    /**
     * 비동기 스트리밍 LLM 요청 발행
     */
    public void requestStreamingResponse(SendMessageRequest request) {
        try {
            // 사용자 메시지 이벤트 발행
            messagePublisher.publishChatEvent(
                request.getChatRoomId(), 
                "USER_MESSAGE_SENT", 
                request.getUserId()
            );
            
            // LLM 요청을 Kafka로 발행
            String requestId = UUID.randomUUID().toString();
            LlmRequest llmRequest = LlmRequest.builder()
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .userMessage(request.getMessage())
                    .requestId(requestId)
                    .build();
            
            messagePublisher.publishLlmRequest(llmRequest);
            log.info("Published LLM request to Kafka: {}", requestId);
            
        } catch (Exception e) {
            log.error("Error requesting streaming LLM response for chat room: {}", request.getChatRoomId(), e);
            throw new RuntimeException("LLM 요청 중 오류가 발생했습니다.", e);
        }
    }
} 