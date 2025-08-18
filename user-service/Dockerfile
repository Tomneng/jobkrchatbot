FROM openjdk:17-jdk-slim

WORKDIR /app

# Gradle wrapper와 build 파일들 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# User service 소스 복사
COPY user-service user-service

# 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 및 빌드
RUN ./gradlew :user-service:build -x test

# JAR 파일 실행
ENTRYPOINT ["java", "-jar", "user-service/build/libs/user-service-0.0.1-SNAPSHOT.jar"]
