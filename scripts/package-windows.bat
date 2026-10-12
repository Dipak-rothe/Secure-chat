@echo off
setlocal enabledelayedexpansion

echo =======================================================
echo Secure Chat - Windows Packaging Script
echo =======================================================

:: Add JDK bin to PATH if not already present
if exist "C:\Program Files\Java\jdk-21.0.12.1\bin" (
    set "PATH=C:\Program Files\Java\jdk-21.0.12.1\bin;%PATH%"
)

cd /d "%~dp0\.."

echo [1/4] Building core and desktop modules...
call gradlew.bat :desktop:installDist
if errorlevel 1 (
    echo [ERROR] Gradle build failed.
    exit /b 1
)

set "APP_NAME=SecureChat"
set "APP_VERSION=0.1.0"
set "INPUT_DIR=desktop\build\install\desktop\lib"
set "MAIN_JAR="
for %%f in ("%INPUT_DIR%\desktop-*.jar") do (
    set "MAIN_JAR=%%~nxf"
)

if "%MAIN_JAR%"=="" (
    echo [ERROR] Could not find desktop JAR in %INPUT_DIR%
    exit /b 1
)

set "OUTPUT_DIR=desktop\build\dist"
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

echo [2/4] Checking for WiX Toolset...
set "PACKAGE_TYPE=app-image"

where candle.exe >nul 2>&1
if %errorlevel% equ 0 (
    set "PACKAGE_TYPE=exe"
    echo Found WiX in PATH. Packaging as Windows Installer EXE.
    goto :RUN_JPACKAGE
)

set "WIX_BIN=%ProgramFiles(x86)%\WiX Toolset v3.11\bin"
if exist "%WIX_BIN%\candle.exe" (
    set "PATH=%WIX_BIN%;%PATH%"
    set "PACKAGE_TYPE=exe"
    echo Found WiX v3.11 in Program Files. Packaging as Windows Installer EXE.
    goto :RUN_JPACKAGE
)

echo [NOTICE] WiX Toolset not found. Falling back to TYPE=app-image.
echo Install WiX 3.11 to build the single-file EXE installer.

:RUN_JPACKAGE
echo [3/4] Running jpackage with type: %PACKAGE_TYPE%...
if "%PACKAGE_TYPE%"=="exe" (
    jpackage --type exe --name "%APP_NAME%" --app-version "%APP_VERSION%" --input "%INPUT_DIR%" --main-jar "%MAIN_JAR%" --main-class com.dipak.desktop.Launcher --dest "%OUTPUT_DIR%" --win-dir-chooser --win-menu --win-shortcut --vendor "Dipak Rothe" --description "SecureChat Messenger"
) else (
    jpackage --type app-image --name "%APP_NAME%" --app-version "%APP_VERSION%" --input "%INPUT_DIR%" --main-jar "%MAIN_JAR%" --main-class com.dipak.desktop.Launcher --dest "%OUTPUT_DIR%" --vendor "Dipak Rothe" --description "SecureChat Messenger"
)

if errorlevel 1 (
    echo [ERROR] jpackage failed.
    exit /b 1
)

echo [4/4] Package complete!
echo Output created in: %OUTPUT_DIR%
dir "%OUTPUT_DIR%"
exit /b 0
