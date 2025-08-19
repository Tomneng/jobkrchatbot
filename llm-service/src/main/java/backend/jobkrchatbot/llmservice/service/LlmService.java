package backend.jobkrchatbot.llmservice.service;

import backend.jobkrchatbot.llmservice.dto.LlmRequest;
import backend.jobkrchatbot.llmservice.dto.LlmResponse;
import backend.jobkrchatbot.llmservice.infrastructure.GptClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final GptClient gptClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    // 채팅방별 SSE Emitter를 저장하는 맵
    private final ConcurrentHashMap<String, SseEmitter> chatRoomEmitters = new ConcurrentHashMap<>();

    /**
     * Keep-alive: 5분마다 ping 이벤트 전송하여 연결 유지
     */
    @Scheduled(fixedRate = 300000) // 5분마다 실행
    public void sendKeepAlive() {
        if (chatRoomEmitters.isEmpty()) {
            return;
        }
        
        log.debug("Sending keep-alive to {} SSE connections", chatRoomEmitters.size());
        
        chatRoomEmitters.entrySet().removeIf(entry -> {
            String chatRoomId = entry.getKey();
            SseEmitter emitter = entry.getValue();
            
            try {
                emitter.send(SseEmitter.event()
                    .name("ping")
                    .data(Map.of(
                        "chatRoomId", chatRoomId,
                        "timestamp", System.currentTimeMillis()
                    )));
                return false; // 성공하면 유지
            } catch (IOException e) {
                log.warn("Failed to send keep-alive to chat room: {}, removing connection", chatRoomId);
                return true; // 실패하면 제거
            }
        });
    }

    /**
     * 채팅방별 직접 SSE 스트리밍 연결 생성
     */
    public SseEmitter createStreamingConnection(String chatRoomId) {
        // 기존 연결이 있다면 재사용
        SseEmitter existingEmitter = chatRoomEmitters.get(chatRoomId);
        if (existingEmitter != null) {
            log.info("Reusing existing SSE emitter for chat room: {}", chatRoomId);
            return existingEmitter;
        }
        
        SseEmitter emitter = new SseEmitter(1800000L); // 30분 타임아웃 (1800초)
        
        // 연결 완료 시 처리
        emitter.onCompletion(() -> {
            log.info("SSE emitter completed for chat room: {}", chatRoomId);
            chatRoomEmitters.remove(chatRoomId);
        });
        
        // 타임아웃 시 처리
        emitter.onTimeout(() -> {
            log.info("SSE emitter timeout for chat room: {}", chatRoomId);
            chatRoomEmitters.remove(chatRoomId);
        });
        
        // 오류 시 처리
        emitter.onError((ex) -> {
            log.error("SSE emitter error for chat room: {}", chatRoomId, ex);
            chatRoomEmitters.remove(chatRoomId);
        });
        
        chatRoomEmitters.put(chatRoomId, emitter);
        log.info("SSE emitter created for chat room: {} with 30min timeout", chatRoomId);
        
        // 연결 확인 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of(
                    "chatRoomId", chatRoomId,
                    "message", "SSE 연결이 설정되었습니다",
                    "timestamp", System.currentTimeMillis()
                )));
        } catch (IOException e) {
            log.error("Error sending connection event", e);
        }
        
        return emitter;
    }

    /**
     * SSE Emitter 등록 (Chat Service에서 호출) - 하위 호환성용
     */
    public SseEmitter registerEmitter(String chatRoomId) {
        return createStreamingConnection(chatRoomId);
    }

    /**
     * Kafka로부터 LLM 요청을 구독하여 처리 (MSA 원칙: 고성능 + 서비스 독립성)
     */
    @KafkaListener(topics = "llm-request", groupId = "llm-service")
    public void handleLlmRequest(String jsonMessage) {
        try {
            log.info("Received LLM request from Kafka");
            
            // 고성능 선택적 파싱: 필요한 필드만 추출
            JsonNode json = objectMapper.readTree(jsonMessage);
            String chatRoomId = json.get("chatRoomId").asText();
            String userId = json.get("userId").asText();
            String userMessage = json.get("userMessage").asText();
            String requestId = json.get("requestId").asText();
            
            log.info("Parsed LLM request: {}", requestId);
            
            // 내부 도메인 객체로 변환 (필요 시에만)
            LlmRequest request = LlmRequest.builder()
                .chatRoomId(chatRoomId)
                .userId(userId)
                .userMessage(userMessage)
                .requestId(requestId)
                .build();
            
            // 비동기로 스트리밍 응답 생성
            generateStreamingResponseAsync(request);
            
        } catch (Exception e) {
            log.error("Error processing LLM request from Kafka: {}", jsonMessage, e);
        }
    }

    /**
     * 비동기 스트리밍 응답 생성 및 SSE로 전송
     */
    @Async
    public CompletableFuture<Void> generateStreamingResponseAsync(LlmRequest request) {
        try {
            log.info("Generating streaming response for request: {}", request.getRequestId());
            
            // 1. 스트리밍 준비
            Flux<String> streamingResponse = prepareStreamingResponse(request);
            SseEmitter emitter = validateAndGetEmitter(request.getChatRoomId());
            
            if (emitter == null) {
                return CompletableFuture.completedFuture(null);
            }
            
            // 2. 스트리밍 시작 이벤트 전송
            if (!sendStartEvent(emitter, request)) {
                return CompletableFuture.completedFuture(null);
            }
            
            // 3. 스트리밍 처리
            processStreamingResponse(streamingResponse, emitter, request);
            
        } catch (Exception e) {
            handleStreamingSetupError(request, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    private Flux<String> prepareStreamingResponse(LlmRequest request) {
        String systemPrompt = createJobSeekerPrompt();
        log.info("GPT API 스트리밍 호출 시작");
        
        return gptClient.generateStreamingResponse(
            request.getUserMessage(), 
            systemPrompt
        );
    }
    
    private SseEmitter validateAndGetEmitter(String chatRoomId) {
        SseEmitter emitter = chatRoomEmitters.get(chatRoomId);
        if (emitter == null) {
            log.error("No SSE emitter found for chat room: {}", chatRoomId);
            return null;
        }
        log.info("SSE emitter 찾음: {}", chatRoomId);
        return emitter;
    }
    
    private boolean sendStartEvent(SseEmitter emitter, LlmRequest request) {
        try {
            emitter.send(SseEmitter.event()
                .name("start")
                .data(Map.of(
                    "requestId", request.getRequestId(),
                    "chatRoomId", request.getChatRoomId()
                )));
            log.info("시작 이벤트 전송 완료");
            return true;
        } catch (IOException e) {
            log.error("Error sending start event", e);
            return false;
        }
    }
    
    private void processStreamingResponse(Flux<String> streamingResponse, SseEmitter emitter, LlmRequest request) {
        StringBuilder fullResponse = new StringBuilder();
        log.info("스트리밍 응답 구독 시작 - requestId: {}", request.getRequestId());
        
        streamingResponse.subscribe(
            chunk -> handleChunk(emitter, fullResponse, chunk),
            error -> handleStreamingError(emitter, request, error),
            () -> handleStreamingComplete(emitter, request, fullResponse)
        );
    }
    
    private void handleChunk(SseEmitter emitter, StringBuilder fullResponse, String chunk) {
        try {
            log.info("GPT API에서 청크 수신: '{}'", chunk);
            
            emitter.send(SseEmitter.event()
                .name("chunk")
                .data(chunk));
            
            fullResponse.append(chunk);
            log.info("청크 전송 완료: '{}'", chunk);
        } catch (IOException e) {
            log.error("Error sending chunk via SSE", e);
        }
    }
    
    private void handleStreamingError(SseEmitter emitter, LlmRequest request, Throwable error) {
        log.error("Error in streaming response", error);
        
        sendErrorEvent(emitter, error);
        publishErrorToKafka(request, error);
    }
    
    private void handleStreamingComplete(SseEmitter emitter, LlmRequest request, StringBuilder fullResponse) {
        try {
            String completeResponse = fullResponse.toString().trim();
            log.info("스트리밍 완료 - requestId: {}, 전체 응답 길이: {}", 
                    request.getRequestId(), completeResponse.length());
            
            sendCompleteEvent(emitter, request, completeResponse);
            publishResponseToKafka(request, completeResponse);
            
        } catch (Exception e) {
            log.error("Error completing streaming", e);
        }
    }
    
    private void sendErrorEvent(SseEmitter emitter, Throwable error) {
        try {
            emitter.send(SseEmitter.event()
                .name("error")
                .data("스트리밍 중 오류가 발생했습니다: " + error.getMessage()));
        } catch (IOException e) {
            log.error("Error sending error event via SSE", e);
        }
    }
    
    private void sendCompleteEvent(SseEmitter emitter, LlmRequest request, String completeResponse) {
        try {
            emitter.send(SseEmitter.event()
                .name("complete")
                .data(Map.of(
                    "requestId", request.getRequestId(),
                    "fullResponse", completeResponse
                )));
            log.info("완료 이벤트 전송됨");
        } catch (IOException e) {
            log.error("Error sending complete event", e);
        }
    }
    
    private void publishErrorToKafka(LlmRequest request, Throwable error) {
        kafkaTemplate.send("llm-error", Map.of(
            "chatRoomId", request.getChatRoomId(),
            "userId", request.getUserId(),
            "requestId", request.getRequestId(),
            "error", error.getMessage()
        ));
    }
    
    private void publishResponseToKafka(LlmRequest request, String completeResponse) {
        kafkaTemplate.send("llm-response", Map.of(
            "chatRoomId", request.getChatRoomId(),
            "userId", request.getUserId(),
            "requestId", request.getRequestId(),
            "message", completeResponse,
            "timestamp", System.currentTimeMillis()
        ));
        log.info("Published LLM response to Kafka for storage");
    }
    
    private void handleStreamingSetupError(LlmRequest request, Exception e) {
        log.error("Error setting up streaming", e);
        publishErrorToKafka(request, e);
    }

    /**
     * 기존 HTTP 엔드포인트용 (하위 호환성)
     */
    public LlmResponse generateResponse(LlmRequest request) {
        try {
            log.info("Generating response for chat room: {}, user: {}", 
                    request.getChatRoomId(), request.getUserId());
            
            String systemPrompt = createJobSeekerPrompt();
            String response = gptClient.generateResponse(request.getUserMessage(), systemPrompt);
            
            return LlmResponse.builder()
                    .response(response)
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error generating response for request: {}", request.getRequestId(), e);
            return LlmResponse.builder()
                    .error("응답 생성 중 오류가 발생했습니다.")
                    .chatRoomId(request.getChatRoomId())
                    .userId(request.getUserId())
                    .requestId(request.getRequestId())
                    .build();
        }
    }
    
    /**
     * 구직자를 위한 맞춤형 시스템 프롬프트를 생성합니다.
     * 이력서 기반 면접 질문 생성과 학습 경로 제안을 위한 프롬프트입니다.
     */
    private String createJobSeekerPrompt() {
        return """
            당신은 구직자의 합격률을 높이기 위한 전문적인 AI 면접 코치입니다.
            
            주요 역할:
            1. 맞춤형 면접 모의질문 생성
            2. 자기 개발 및 학습 경로 제안
            3. 구체적이고 실용적인 조언 제공
            
            응답 형식:
            사용자가 이력서 정보(경력, 직무, 기술 스킬)를 입력하면 다음 두 가지를 제공하세요:
            
            📝 맞춤형 면접 질문 (5개)
            - 입력된 경력과 기술 스킬에 기반한 실제 면접에서 나올 법한 질문
            - 기술적 깊이와 경험을 평가하는 질문
            - 구체적이고 실용적인 상황 기반 질문
            
            🚀 자기 개발 학습 경로
            - 기술 스택 심화 방안
            - 관련 프로젝트 경험 쌓기 방법
            - 커뮤니케이션 스킬 강화 방안
            - 합격률 향상을 위한 구체적인 액션 플랜
            
            응답은 한국어로 작성하고, 구체적이고 실용적인 내용으로 구성해주세요.
            """;
    }
} 