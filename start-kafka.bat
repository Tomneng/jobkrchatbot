@echo off
setlocal enabledelayedexpansion

REM 현재 타임스탬프를 기반으로 Broker ID 생성
for /f "tokens=1-3 delims=:.," %%a in ("%time%") do (
    set hour=%%a
    set minute=%%b
    set second=%%c
)

REM 초 단위로 Broker ID 생성
set /a BROKER_ID_1=(%hour%*3600 + %minute%*60 + %second%) %% 1000 + 1
set /a BROKER_ID_2=(%hour%*3600 + %minute%*60 + %second%) %% 1000 + 2

echo Generated Broker IDs: %BROKER_ID_1%, %BROKER_ID_2%

REM 환경 변수 설정
set KAFKA_BROKER_ID_1=%BROKER_ID_1%
set KAFKA_BROKER_ID_2=%BROKER_ID_2%

REM Docker Compose 실행
docker-compose up --build -d
