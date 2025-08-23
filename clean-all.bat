@echo off
echo Cleaning all Docker containers and volumes...

REM Spring Cloud Gateway 및 관련 Java 프로세스 종료
echo Stopping Java processes...
taskkill /F /IM java.exe 2>nul
echo Java processes stopped.

REM 모든 컨테이너 중지 및 삭제
docker-compose down -v

REM 모든 Docker 볼륨 삭제
docker volume prune -f

REM 모든 Docker 네트워크 삭제
docker network prune -f

REM 모든 Docker 이미지 삭제 (선택사항)
echo.
set /p choice="Delete all Docker images? (y/n): "
if /i "%choice%"=="y" (
    docker image prune -a -f
    echo All Docker images deleted.
) else (
    echo Docker images preserved.
)

REM 임시 디렉토리 정리 (Windows)
echo Cleaning temporary directories...
if exist "C:\tmp\zookeeper" rmdir /s /q "C:\tmp\zookeeper"
if exist "C:\tmp\kafka-logs" rmdir /s /q "C:\tmp\kafka-logs"
if exist "C:\tmp\redis" rmdir /s /q "C:\tmp\redis"
echo Temporary directories cleaned.

echo.
echo Cleanup completed!
echo.
echo To start fresh, run: start-kafka.bat
echo Or use: docker-compose up -d
pause
