package backend.jobkrchatbot.application.service;

import backend.jobkrchatbot.application.dto.ChatRequest;
import backend.jobkrchatbot.application.dto.ChatResponse;
import backend.jobkrchatbot.application.port.LlmPort;
import backend.jobkrchatbot.domain.entity.Resume;
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

    private final LlmPort llmPort;
    
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
            
            String systemMessage;
            if (isFirstMessage) {
                systemMessage = "당신은 취업 상담을 도와주는 전문가입니다. 사용자의 이력서 정보를 바탕으로 맞춤형 조언을 제공해주세요.";
            } else {
                systemMessage = "당신은 취업 상담을 도와주는 전문가입니다. 이전 대화 내용을 참고하여 일관성 있는 답변을 제공해주세요.";
            }
            
            // LLM을 통한 응답 생성
            String response = llmPort.generateResponse(request.getMessage(), systemMessage);
            
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