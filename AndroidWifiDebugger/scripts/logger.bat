@echo off
rem Usage: call logger.bat LEVEL "message"
if not defined LOG_DIR set "LOG_DIR=%~dp0..\logs"
if not defined LOG_FILE set "LOG_FILE=%LOG_DIR%\adb.log"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>&1
set "LOG_LEVEL=%~1"
set "LOG_MESSAGE=%~2"
for /f "tokens=1-3 delims=/. " %%a in ("%date%") do set "LOG_DATE=%date%"
>>"%LOG_FILE%" echo [%date% %time%] [!LOG_LEVEL!] Serial=!USB_SERIAL! WiFi=!WIFI_SERIAL! IP=!PHONE_IP! - !LOG_MESSAGE!
exit /b 0

