package chatservice.infrastructure.adapter;

import chatservice.domain.port.LlmService;
import chatservice.infrastructure.client.LlmClient;
import chatservice.infrastructure.client.dto.LlmRequest;
import chatservice.infrastructure.client.dto.LlmResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpLlmService implements LlmService {

    private final LlmClient llmClient;

    @Override
    public String generateResponse(String chatRoomId, String userId, String userMessage, String requestId) {
        log.info("Generating LLM response for chat room: {}, user: {}", chatRoomId, userId);

        LlmRequest request = LlmRequest.builder()
                .chatRoomId(chatRoomId)
                .userId(userId)
                .userMessage(userMessage)
                .requestId(requestId)
                .build();

        LlmResponse response = llmClient.generateResponse(request);

        if (response.getError() != null) {
            log.error("LLM service returned error: {}", response.getError());
            return "죄송합니다. 현재 서비스에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }

        return response.getResponse();
    }
} 