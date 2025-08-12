package backend.jobkrchatbot.application.service;

import backend.jobkrchatbot.application.dto.ChatRequest;
import backend.jobkrchatbot.application.dto.ChatResponse;
<<<<<<< HEAD
=======
import backend.jobkrchatbot.domain.model.LlmPrompt;
>>>>>>> c03c19066653ad63ce423430e01af23d6cd7f95c
import backend.jobkrchatbot.domain.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final LlmService llmService;
    
    // 채팅방별 히스토리를 메모리에 저장 (실제 운영에서는 Redis나 DB 사용 권장)
    private final Map<String, List<String>> chatHistories = new ConcurrentHashMap<>();

    public ChatResponse generateChatResponse(ChatRequest request, String chatRoomId) {
        try {
            // 채팅방 히스토리 가져오기
            List<String> chatHistory = chatHistories.computeIfAbsent(chatRoomId, k -> new ArrayList<>());
            
            // 사용자 메시지를 히스토리에 추가
            chatHistory.add("User: " + request.getMessage());
            
            // 첫 메시지인지 확인
            boolean isFirstMessage = chatHistory.size() == 1;
            
            // 의도 분석 및 시스템 메시지 생성
            String systemMessage = LlmPrompt.generateSystemMessage(request.getMessage(), isFirstMessage);
            
<<<<<<< HEAD
            // 도메인 서비스를 통한 응답 생성
            String response = llmService.generateChatResponse(request.getMessage(), systemMessage);
=======
            // RAG 방식: 채팅 히스토리와 시스템 메시지를 조합
            String contextWithHistory = buildContextWithHistory(chatHistory, systemMessage);
            
            // 도메인 서비스를 통한 응답 생성
            String response = llmService.generateChatResponse(request.getMessage(), contextWithHistory);
>>>>>>> c03c19066653ad63ce423430e01af23d6cd7f95c
            
            // 응답을 히스토리에 추가
            chatHistory.add("Assistant: " + response);
            
            // 히스토리 크기 제한 (최근 10개 대화 유지)
            if (chatHistory.size() > 20) {
                List<String> recentHistory = chatHistory.subList(chatHistory.size() - 20, chatHistory.size());
                chatHistories.put(chatRoomId, new ArrayList<>(recentHistory));
            }
            
            return ChatResponse.builder()
                    .message(response)
                    .chatRoomId(chatRoomId)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error generating chat response", e);
            return ChatResponse.builder()
                    .message("죄송합니다. 응답을 생성하는 중 오류가 발생했습니다.")
                    .chatRoomId(chatRoomId)
                    .build();
        }
    }
    
    /**
     * 채팅 히스토리와 시스템 메시지를 조합하여 RAG 컨텍스트 생성
     */
    private String buildContextWithHistory(List<String> chatHistory, String systemMessage) {
        StringBuilder context = new StringBuilder();
        context.append(systemMessage).append("\n\n");
        
        // 이전 대화 히스토리 추가 (최근 5개 대화만 포함)
        int startIndex = Math.max(0, chatHistory.size() - 10);
        for (int i = startIndex; i < chatHistory.size(); i++) {
            context.append(chatHistory.get(i)).append("\n");
        }
        
        context.append("\n위의 대화 내용을 참고하여 사용자의 질문에 답변해주세요.");
        
        return context.toString();
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
} 