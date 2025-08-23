#!/bin/bash

# 현재 타임스탬프를 기반으로 Broker ID 생성
TIMESTAMP=$(date +%s)
BROKER_ID_1=$((TIMESTAMP % 1000 + 1))
BROKER_ID_2=$((TIMESTAMP % 1000 + 2))

echo "Generated Broker IDs: $BROKER_ID_1, $BROKER_ID_2"

# 환경 변수 설정
export KAFKA_BROKER_ID_1=$BROKER_ID_1
export KAFKA_BROKER_ID_2=$BROKER_ID_2

# Docker Compose 실행
docker-compose up --build
