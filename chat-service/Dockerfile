FROM openjdk:17-jdk-slim

WORKDIR /app

# Gradle wrapper와 build 파일들 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Chat service 소스 복사
COPY chat-service chat-service

# 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 및 빌드
RUN ./gradlew :chat-service:build -x test

# JAR 파일 실행
ENTRYPOINT ["java", "-jar", "chat-service/build/libs/chat-service-0.0.1-SNAPSHOT.jar"]
