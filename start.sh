#!/bin/bash

echo "🚀 JobKR Chatbot MSA 시스템을 시작합니다..."

# 환경 변수 확인
if [ -z "$ANTHROPIC_API_KEY" ]; then
    echo "⚠️  ANTHROPIC_API_KEY 환경 변수가 설정되지 않았습니다."
    echo "   export ANTHROPIC_API_KEY='your-api-key-here' 명령어로 설정해주세요."
    exit 1
fi

echo "📦 Docker 이미지들을 빌드하고 서비스를 시작합니다..."
docker-compose up --build -d

echo "⏳ 서비스들이 시작될 때까지 기다립니다..."
sleep 30

echo "✅ 모든 서비스가 시작되었습니다!"
echo ""
echo "🌐 접근 가능한 서비스들:"
echo "   - API Gateway: http://localhost:8000"
echo "   - Chat Service: http://localhost:8081"
echo "   - LLM Service: http://localhost:8082"
echo "   - User Service: http://localhost:8083"
echo "   - Kafka UI: http://localhost:8080"
echo ""
echo "📊 서비스 상태 확인:"
echo "   docker-compose ps"
echo ""
echo "🛑 서비스 중지:"
echo "   docker-compose down"
