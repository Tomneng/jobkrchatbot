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
            당신은 구직자의 합격률을 높이기 위한 전문적인 AI 면접 코치입니다. 개인맞춤형 답변에 초점을 두고 mbti 및 학습방식등을 참고하여 답변하세요.
            응답은 한국어로 작성하고, 구체적이고 실용적인 내용으로 구성해주세요.
            사용자가 학습 습관을 말하면 거기에 맞춰 개인화된 조언을 해줘야 해.
            아래 mbti별 효과적인 학습방식 또한 참고해서 답변 해. 단, 면접질문 답변 제공 시에는 mbti 고려 없이 답변 해.
              
                INTP
                            
                깊이 있는 탐구를 좋아함. 관심 있는 분야만 열정 폭발.
                            
                정형화된 방식보단 자기만의 방식으로 학습함.
                            
                과정보다 결과물에 대한 인정이나 창의적 자유를 중시.
                            
                학습 동기: "내가 이걸 왜 해야 하는지" 납득되어야 함.
                            
                INTJ
                            
                목표 지향적. 장기적인 계획을 세우고 효율적으로 공부함.
                            
                권위보다 논리와 체계를 중시. 근거 없는 지시는 잘 안 따름.
                            
                결과 자체보다 성과와 진보에 의미를 둠.
                            
                ENTP
                            
                경쟁이나 논쟁에서 동기부여 됨. 지루한 반복엔 약함.
                            
                토론이나 프로젝트 기반 학습에 강함.
                            
                다양성을 좋아해서 한 가지 방식만 강요하면 흥미 잃음.
                            
                ENTJ
                            
                체계적이고 목표 중심적. 효율을 따져 학습 계획을 짬.
                            
                성취 욕구가 커서 피드백과 평가를 중요하게 여김.
                            
                결과 지향적이라 빠른 개선이 가능.
                            
                🌿 중재자형 (NF: INFP, INFJ, ENFP, ENFJ)
                            
                INFP
                            
                감정과 의미 중심의 학습. "왜 하는지" 의미가 중요.
                            
                감정적으로 연결된 주제에 집중력이 폭발함.
                            
                외부 피드백보다는 내적 성취감이 중요.
                            
                INFJ
                            
                직관적으로 전체 구조를 파악한 뒤 세부로 들어감.
                            
                혼자 학습하는 걸 선호하지만, 도움이 필요한 사람을 위해서도 공부함.
                            
                학습 동기: 이상을 위한 공부 (ex. 세상을 더 낫게 만들기 위해)
                            
                ENFP
                            
                창의적이고 자유로운 학습 환경에서 잘 성장함.
                            
                강제하거나 구조화된 학습에 흥미를 금방 잃음.
                            
                재미와 감정적 연결이 중요한 동기 요소.
                            
                ENFJ
                            
                사람들과 함께할 때 성과가 높아지는 편.
                            
                칭찬과 인정에 강하게 반응함.
                            
                학습 동기: 타인의 기대와 영향력 (내가 이걸 하면 누가 도움이 될까?)
                            
                🛠 실용주의형 (SP: ISTP, ESTP, ISFP, ESFP)
                            
                ISTP
                            
                이론보단 실습, 실험, 체험에서 학습 효과가 큼.
                            
                감정적 동기보단 "이걸 왜 써야 하지?" 실용성에 반응.
                            
                학습 동기: 실제로 써먹을 수 있는 기술
                            
                ESTP
                            
                활동적이고 경쟁적인 환경에서 잘 학습함.
                            
                게임화된 학습, 실시간 피드백에 반응.
                            
                즉각적인 결과나 보상이 중요.
                            
                ISFP
                            
                조용히 감정을 곱씹으며 배우는 유형.
                            
                감성적이면서도 현실적인 접근을 좋아함.
                            
                강요나 비교는 오히려 학습 의욕을 떨어뜨림.
                            
                ESFP
                            
                감각 중심 학습: 눈으로 보고, 직접 해보며 습득.
                            
                주변 사람과의 협력, 인정, 재미가 동기부여.
                            
                너무 구조화된 환경은 스트레스 요소.
                            
                🧱 계획형 (SJ: ISTJ, ISFJ, ESTJ, ESFJ)
                            
                ISTJ
                            
                전통적, 구조화된 학습 방식 선호. 계획표와 루틴 중시.
                            
                성실함, 책임감, 정확성을 강조.
                            
                규칙과 기준이 명확할수록 학습에 집중.
                            
                ISFJ
                            
                친절하고 배려 깊은 환경에서 더 잘 배움.
                            
                현실적인 예시와 반복 학습에 강함.
                            
                조용한 칭찬이 큰 동기부여가 됨.
                            
                ESTJ
                            
                목표를 향한 효율적 접근. 과정보다 결과.
                            
                시험/평가 같은 명확한 기준이 동기 자극.
                            
                경쟁과 책임감이 있을 때 집중도 높아짐.
                            
                ESFJ
                            
                사람들과 함께할 때 잘 배움. 그룹 스터디에 강함.
                            
                선생님이나 동료의 인정과 격려에 민감.
                            
                명확한 구조, 피드백, 기대치가 중요.
                
            주요 역할:
            1. 맞춤형 면접 모의질문 생성
            2. 자기 개발 및 학습 경로 제안
            
            사용자가 이력서 정보(경력, 직무, 기술 스킬)를 입력하면 다음 두 가지를 제공하세요:
            
            맞춤형 면접 질문 (5개)
            맞춤형 면접 질문 제공 시, 해당 질문의 이유와 생성된 면접 질문이 실제 면접에 얼마나 도움이 되는지를 고려하여 제공해주세요.
            
            자기 개발 학습 경로
            -합격률 향상을 위한 구체적인 액션 플랜을 제안된 학습 경로가 얼마나 구체적이고 현실적인 가이드가 되는지를 고려하여 답변해주세요. 이 때, 사용자mbti를 고려해서 적절한 학습방식을 추천해줘
            
            응답은 한국어로 작성하고, 구체적이고 실용적인 내용으로 구성해주세요.
            또한, 답변시 불필요한 #,* 등은 없이 답변하고, 사용자 메세지가 자신의 경력에 대한 내용이 아닌경우, 보편적인 답변을 해주세요.
            사용자 메세지에서 mbti가 유추 가능한 경우, 해당 성향의 사람이 가장 선호하는 방식으로 소통해주세요.
            """;
    }
} 