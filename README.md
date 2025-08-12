# JobKR Chatbot MSA

취업 상담 챗봇을 MSA(Microservice Architecture)로 구현한 프로젝트입니다.

## 🏗️ 아키텍처

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Client   │    │ API Gateway │    │ Chat Service│
│             │◄──►│   (8080)   │◄──►│   (8081)   │
└─────────────┘    └─────────────┘    └─────────────┘
                          │                   │
                          ▼                   ▼
                   ┌─────────────┐    ┌─────────────┐
                   │User Service │    │ LLM Service │
                   │   (8083)   │    │   (8082)   │
                   └─────────────┘    └─────────────┘
                                              │
                                              ▼
                                     ┌─────────────┐
                                     │   Claude   │
                                     │     API    │
                                     └─────────────┘
```

## 🚀 서비스 구성

### 1. **Chat Service (8081)**
- 사용자 채팅 세션 관리
- 채팅 히스토리 저장/조회
- Kafka를 통한 LLM 요청 전송

### 2. **LLM Service (8082)**
- Claude API 연동
- Kafka를 통한 비동기 요청 처리
- AI 응답 생성

### 3. **User Service (8083)**
- 사용자 인증/인가
- 사용자 프로필 관리

### 4. **API Gateway (8080)**
- 모든 서비스의 진입점
- 라우팅 및 로드 밸런싱

### 5. **Common Library**
- 공통 DTO 및 응답 클래스
- 모든 서비스에서 공유

## 🛠️ 기술 스택

- **Framework**: Spring Boot 3.5.4
- **Message Broker**: Apache Kafka
- **Database**: PostgreSQL
- **Cache**: Redis
- **API Gateway**: Spring Cloud Gateway
- **Language**: Java 17

## 📋 실행 방법

### 1. 인프라 실행
```bash
docker-compose up -d
```

### 2. 각 서비스 실행
```bash
# Chat Service
cd chat-service && ./gradlew bootRun

# LLM Service  
cd llm-service && ./gradlew bootRun

# User Service
cd user-service && ./gradlew bootRun

# API Gateway
cd api-gateway && ./gradlew bootRun
```

### 3. 환경 변수 설정
```bash
export ANTHROPIC_API_KEY="your-claude-api-key"
```

## 🔄 비동기 통신 흐름

1. **사용자 메시지 전송**
   ```
   Client → API Gateway → Chat Service
   ```

2. **LLM 요청 처리**
   ```
   Chat Service → Kafka → LLM Service
   ```

3. **AI 응답 생성**
   ```
   LLM Service → Claude API → LLM Service
   ```

4. **응답 전달**
   ```
   LLM Service → Kafka → Chat Service → Client
   ```

## 📊 모니터링

- **Kafka UI**: http://localhost:8080
- **Chat Service**: http://localhost:8081
- **LLM Service**: http://localhost:8082
- **User Service**: http://localhost:8083
- **API Gateway**: http://localhost:8080

## 🎯 주요 특징

- **비동기 처리**: Kafka를 통한 메시지 기반 통신
- **독립 배포**: 각 서비스별 독립적인 배포 가능
- **확장성**: 서비스별 독립적인 스케일링
- **장애 격리**: 한 서비스의 장애가 다른 서비스에 영향 최소화

## 🔧 개발 환경

- **IDE**: IntelliJ IDEA / VS Code
- **Build Tool**: Gradle
- **Container**: Docker & Docker Compose
- **OS**: Windows / macOS / Linux 