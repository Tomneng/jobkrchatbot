FROM openjdk:17-jdk-slim

WORKDIR /app

# Gradle wrapper와 build 파일들 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Common lib 복사
COPY common-lib common-lib

# API Gateway 소스 복사
COPY api-gateway api-gateway

# 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 및 빌드
RUN ./gradlew :api-gateway:build -x test

# JAR 파일 실행
ENTRYPOINT ["java", "-jar", "api-gateway/build/libs/api-gateway-0.0.1-SNAPSHOT.jar"]
