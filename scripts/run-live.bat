@echo off
setlocal enabledelayedexpansion

echo =======================================================
echo Secure Chat - Live Development ^& Real-Time Runner
echo =======================================================

REM Navigate to project root
cd /d "%~dp0\.."

REM Configure JDK 21 if available
if exist "C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
)

REM 1. Check if Server is already running on Port 8090
netstat -ano | findstr ":8090" | findstr "LISTENING" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [1/3] Starting Spring Boot Server on port 8090 ^(with Live DevTools Reload^)...
    start "SecureChat Backend Server (Port 8090)" cmd /k "cd /d "%CD%\server" && mvn spring-boot:run"
    echo Waiting 10 seconds for server to initialize...
    timeout /t 10 /nobreak >nul
) else (
    echo [1/3] Backend Server is already running on port 8090.
)

REM 2. Ask if user wants to start the Pinggy Public Tunnel for PC 2
echo.
set /p START_TUNNEL="[2/3] Start public Pinggy tunnel for other PCs? (Y/N, default=N): "
if /i "!START_TUNNEL!"=="Y" (
    echo Starting Pinggy Tunnel on Port 443...
    start "SecureChat Public Tunnel" cmd /k "ssh -p 443 -R 80:localhost:8090 a.pinggy.io"
)

:RUN_CLIENT
echo.
echo [3/3] Compiling latest changes and launching SecureChat Desktop Live...
call gradlew.bat :desktop:run

echo.
echo =======================================================
echo Desktop app closed.
echo Make any code changes you want in core/ or desktop/!
echo Press ENTER to recompile ^& relaunch immediately, or Ctrl+C to exit.
echo =======================================================
pause >nul
goto RUN_CLIENT
