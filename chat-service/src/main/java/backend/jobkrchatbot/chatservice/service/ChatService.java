package backend.jobkrchatbot.chatservice.service;

import backend.jobkrchatbot.common.dto.ChatRequest;
import backend.jobkrchatbot.common.dto.ChatResponse;
import backend.jobkrchatbot.common.dto.LlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final KafkaTemplate<String, LlmRequest> kafkaTemplate;
    private final Map<String, List<String>> chatHistories = new ConcurrentHashMap<>();
    
    private static final String LLM_REQUEST_TOPIC = "llm.requests";

    public ChatResponse processChatMessage(ChatRequest request) {
        try {
            // 채팅방 히스토리 가져오기
            List<String> chatHistory = chatHistories.computeIfAbsent(request.getChatRoomId(), k -> new java.util.ArrayList<>());
            
            // 사용자 메시지를 히스토리에 추가
            chatHistory.add("User: " + request.getMessage());
            
            // 첫 메시지인지 확인
            boolean isFirstMessage = chatHistory.size() == 1;
            
            String systemMessage;
            if (isFirstMessage) {
                systemMessage = "당신은 취업 상담을 도와주는 전문가입니다. 사용자의 이력서 정보를 바탕으로 맞춤형 조언을 제공해주세요.";
            } else {
                systemMessage = "당신은 취업 상담을 도와주는 전문가입니다. 이전 대화 내용을 참고하여 일관성 있는 답변을 제공해주세요.";
            }
            
            // LLM 서비스로 요청 전송 (비동기)
            LlmRequest llmRequest = LlmRequest.builder()
                    .userMessage(request.getMessage())
                    .systemMessage(systemMessage)
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
            
            CompletableFuture<SendResult<String, LlmRequest>> future = kafkaTemplate.send(LLM_REQUEST_TOPIC, request.getRequestId(), llmRequest);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("LLM request sent successfully: {}", request.getRequestId());
                } else {
                    log.error("Failed to send LLM request: {}", request.getRequestId(), ex);
                }
            });
            
            // 임시 응답 반환 (실제로는 WebSocket을 통해 비동기로 응답)
            return ChatResponse.builder()
                    .message("메시지를 처리 중입니다. 잠시만 기다려주세요...")
                    .chatRoomId(request.getChatRoomId())
                    .requestId(request.getRequestId())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ChatResponse.builder()
                    .message("죄송합니다. 메시지 처리 중 오류가 발생했습니다.")
                    .chatRoomId(request.getChatRoomId())
                    .requestId(request.getRequestId())
                    .build();
        }
    }
    
    public ChatResponse getChatHistory(String chatRoomId) {
        List<String> chatHistory = chatHistories.get(chatRoomId);
        if (chatHistory == null || chatHistory.isEmpty()) {
            return ChatResponse.builder()
                    .message("채팅 히스토리가 없습니다.")
                    .chatRoomId(chatRoomId)
                    .build();
        }
        
        String historyText = String.join("\n", chatHistory);
        return ChatResponse.builder()
                .message(historyText)
                .chatRoomId(chatRoomId)
                .build();
    }
    
    public void endChat(String chatRoomId) {
        chatHistories.remove(chatRoomId);
        log.info("Chat room {} ended", chatRoomId);
    }
    
    // LLM 응답을 받아서 채팅 히스토리에 추가하는 메서드
    public void handleLlmResponse(String chatRoomId, String response) {
        List<String> chatHistory = chatHistories.get(chatRoomId);
        if (chatHistory != null) {
            chatHistory.add("Assistant: " + response);
            
            // 히스토리 크기 제한 (최근 20개 대화 유지)
            if (chatHistory.size() > 20) {
                List<String> recentHistory = chatHistory.subList(chatHistory.size() - 20, chatHistory.size());
                chatHistories.put(chatRoomId, new java.util.ArrayList<>(recentHistory));
            }
        }
    }
} 