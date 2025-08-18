package backend.jobkrchatbot.llmservice.service;

import backend.jobkrchatbot.llmservice.dto.LlmRequest;
import backend.jobkrchatbot.llmservice.dto.LlmResponse;
import backend.jobkrchatbot.llmservice.infrastructure.GptClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
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
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 채팅방별 SSE Emitter를 저장하는 맵
    private final ConcurrentHashMap<String, SseEmitter> chatRoomEmitters = new ConcurrentHashMap<>();

    /**
     * 채팅방별 직접 SSE 스트리밍 연결 생성
     */
    public SseEmitter createStreamingConnection(String chatRoomId) {
        SseEmitter emitter = new SseEmitter(60000L); // 60초 타임아웃
        
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
        log.info("SSE emitter created for chat room: {}", chatRoomId);
        
        return emitter;
    }

    /**
     * SSE Emitter 등록 (Chat Service에서 호출) - 하위 호환성용
     */
    public SseEmitter registerEmitter(String chatRoomId) {
        return createStreamingConnection(chatRoomId);
    }

    /**
     * Kafka로부터 LLM 요청을 구독하여 처리
     */
    @KafkaListener(topics = "llm-request", groupId = "llm-service")
    public void handleLlmRequest(String requestJson) {
        try {
            log.info("Received LLM request from Kafka: {}", requestJson);
            
            // JSON 문자열을 LlmRequest 객체로 변환
            LlmRequest request = objectMapper.readValue(requestJson, LlmRequest.class);
            log.info("Parsed LLM request: {}", request.getRequestId());
            
            // 비동기로 스트리밍 응답 생성
            generateStreamingResponseAsync(request);
            
        } catch (Exception e) {
            log.error("Error processing LLM request from Kafka: {}", requestJson, e);
        }
    }

    /**
     * 비동기 스트리밍 응답 생성 및 SSE로 전송
     */
    @Async
    public CompletableFuture<Void> generateStreamingResponseAsync(LlmRequest request) {
        try {
            log.info("Generating streaming response for request: {}", request.getRequestId());
            
            // 구직자 맞춤형 프롬프트 생성
            String systemPrompt = createJobSeekerPrompt();
            
            // 실제 GPT API 스트리밍 호출
            log.info("GPT API 스트리밍 호출 시작");
            Flux<String> streamingResponse = gptClient.generateStreamingResponse(
                request.getUserMessage(), 
                systemPrompt
            );
            
            // SSE Emitter 가져오기
            SseEmitter emitter = chatRoomEmitters.get(request.getChatRoomId());
            if (emitter == null) {
                log.error("No SSE emitter found for chat room: {}", request.getChatRoomId());
                return CompletableFuture.completedFuture(null);
            }
            log.info("SSE emitter 찾음: {}", request.getChatRoomId());
            
            // 스트리밍 시작 이벤트 전송
            try {
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data(Map.of(
                        "requestId", request.getRequestId(),
                        "chatRoomId", request.getChatRoomId()
                    )));
                log.info("시작 이벤트 전송 완료");
            } catch (IOException e) {
                log.error("Error sending start event", e);
                return CompletableFuture.completedFuture(null);
            }
            
            // 스트리밍 응답을 SSE로 실시간 전송
            StringBuilder fullResponse = new StringBuilder();
            
            log.info("스트리밍 응답 구독 시작 - requestId: {}", request.getRequestId());
            
            streamingResponse.subscribe(
                // onNext: 각 청크 처리
                chunk -> {
                    try {
                        log.info("GPT API에서 청크 수신: '{}'", chunk);
                        
                        // SSE로 실시간 청크 전송
                        emitter.send(SseEmitter.event()
                            .name("chunk")
                            .data(chunk));
                        
                        // 전체 응답 누적
                        fullResponse.append(chunk);
                        
                        log.info("청크 전송 완료: '{}'", chunk);
                    } catch (IOException e) {
                        log.error("Error sending chunk via SSE", e);
                    }
                },
                // onError: 오류 처리
                error -> {
                    log.error("Error in streaming response", error);
                    
                    try {
                        // 오류 이벤트를 SSE로 전송
                        emitter.send(SseEmitter.event()
                            .name("error")
                            .data("스트리밍 중 오류가 발생했습니다: " + error.getMessage()));
                    } catch (IOException e) {
                        log.error("Error sending error event via SSE", e);
                    }
                    
                    // 오류 이벤트를 Kafka로 발행
                    kafkaTemplate.send("llm-error", Map.of(
                        "chatRoomId", request.getChatRoomId(),
                        "userId", request.getUserId(),
                        "requestId", request.getRequestId(),
                        "error", error.getMessage()
                    ));
                },
                // onComplete: 완료 처리
                () -> {
                    try {
                        String completeResponse = fullResponse.toString().trim();
                        log.info("스트리밍 완료 - requestId: {}, 전체 응답 길이: {}", 
                                request.getRequestId(), completeResponse.length());
                        log.info("전체 응답 내용: '{}'", completeResponse);
                        
                        // 완료 이벤트를 SSE로 전송
                        emitter.send(SseEmitter.event()
                            .name("complete")
                            .data(Map.of(
                                "requestId", request.getRequestId(),
                                "fullResponse", completeResponse
                            )));
                        
                        log.info("완료 이벤트 전송됨");
                        
                        // SSE 완료
                        emitter.complete();
                        
                        // 저장용 이벤트를 Kafka로 발행
                        kafkaTemplate.send("llm-response", Map.of(
                            "chatRoomId", request.getChatRoomId(),
                            "userId", request.getUserId(),
                            "requestId", request.getRequestId(),
                            "message", completeResponse,
                            "timestamp", System.currentTimeMillis()
                        ));
                        
                        log.info("Published LLM response to Kafka for storage");
                        
                    } catch (Exception e) {
                        log.error("Error completing streaming", e);
                    }
                }
            );
            
        } catch (Exception e) {
            log.error("Error setting up streaming", e);
            
            // 오류 이벤트를 Kafka로 발행
            kafkaTemplate.send("llm-error", Map.of(
                "chatRoomId", request.getChatRoomId(),
                "userId", request.getUserId(),
                "requestId", request.getRequestId(),
                "error", e.getMessage()
            ));
        }
        
        return CompletableFuture.completedFuture(null);
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