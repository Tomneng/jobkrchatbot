# JobKR Chatbot - MSA Architecture

## 개요
Spring Boot 기반의 MSA(Microservice Architecture) 채팅봇 시스템입니다.

## 아키텍처
<img width="993" height="414" alt="image" src="https://github.com/user-attachments/assets/efa78dd0-8220-402b-b789-a9c38c8f268d" />


## Spring Cloud Gateway 기능

### 주요 기능
- **라우팅**: 각 마이크로서비스로의 요청 라우팅
- **Circuit Breaker**: Resilience4j를 사용한 장애 격리
- **Rate Limiting**: Redis 기반 요청 제한
- **Retry**: 자동 재시도 메커니즘
- **CORS**: Cross-Origin 요청 지원
- **Security Headers**: 보안 헤더 자동 추가
- **Logging**: 요청/응답 로깅

### 라우팅 규칙
```
/api/chat/** → Chat Service (8081)
/api/llm/** → LLM Service (8082)
/health/** → Health Check
/api-docs/** → API Documentation
```

### Circuit Breaker 설정
- **Chat Service**: 10개 요청 중 50% 실패 시 Circuit Open
- **LLM Service**: 10개 요청 중 50% 실패 시 Circuit Open
- **Fallback**: 서비스 장애 시 적절한 응답 제공

### Rate Limiting 설정
- **Chat Service**: 초당 10개 요청, 최대 20개 버스트
- **LLM Service**: 초당 5개 요청, 최대 10개 버스트

## 실행 방법

### 1. 환경 변수 설정
```bash
# .env 파일 생성
POSTGRES_DB=jobkrchatbot
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
REDIS_PORT=6379
OPENAI_API_KEY=your_openai_api_key
CLAUDE_API_KEY=your_claude_api_key
```

### 2. Docker Compose로 실행
```bash
docker-compose up --build -d
```

### 3. 개별 서비스 실행
```bash
# API Gateway
cd api-gateway
./gradlew bootRun

# Chat Service
cd chat-service
./gradlew bootRun

# LLM Service
cd llm-service
./gradlew bootRun
```

## API 엔드포인트

### API Gateway (8080)
- `GET /actuator/health` - 게이트웨이 상태 확인
- `GET /actuator/gateway` - 라우트 정보
- `GET /health/gateway` - 게이트웨이 헬스체크
- `GET /health/circuit-breakers` - Circuit Breaker 상태

### Chat Service (8081)
- `POST /api/chat/start` - 채팅 시작
- `POST /api/chat/send` - 메시지 전송
- `GET /api/chat/history/{roomId}` - 채팅 기록 조회

### LLM Service (8082)
- `POST /api/llm/chat` - AI 응답 생성

## 모니터링

### Actuator 엔드포인트
- `/actuator/health` - 전체 시스템 상태
- `/actuator/gateway` - 게이트웨이 라우트 정보
- `/actuator/circuitbreakers` - Circuit Breaker 상태
- `/actuator/ratelimiters` - Rate Limiter 상태

### 로깅
- Spring Cloud Gateway: DEBUG 레벨
- 요청/응답 로깅: 자동으로 모든 요청 기록
- 성능 모니터링: 응답 시간 측정

## 개발 환경

### 요구사항
- Java 17+
- Gradle 7.6+
- Docker & Docker Compose
- Redis (Rate Limiting용)

### 프로젝트 구조
```
jobkrchatbot/
├── api-gateway/          # Spring Cloud Gateway
├── chat-service/         # 채팅 서비스
├── llm-service/          # LLM 서비스
├── compose.yaml          # Docker Compose 설정
└── README.md
```

## 문제 해결

### Kafka 에러
```bash
# 기존 프로세스 종료
taskkill /F /IM java.exe

# 데이터 정리
rmdir /s /q C:\tmp\zookeeper
rmdir /s /q C:\tmp\kafka-logs

# clean-all.bat 실행
clean-all.bat
```

### Redis 연결 에러
```bash
# Redis 컨테이너 상태 확인
docker ps | grep redis

# Redis 재시작
docker-compose restart redis
```

## 성능 최적화

### Gateway 설정
- Circuit Breaker로 장애 격리
- Rate Limiting으로 과부하 방지
- Retry 메커니즘으로 일시적 장애 대응
- 보안 헤더 자동 추가

### 모니터링
- Actuator를 통한 실시간 상태 모니터링
- 상세한 로깅으로 디버깅 지원
- 성능 메트릭 수집 
