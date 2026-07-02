@echo off
if /i "%~1"=="run" goto :run
exit /b 2

:run
title Android WiFi Debugger - Monitoring
:monitor_loop
set "WIFI_STATE="
if defined WIFI_SERIAL for /f "tokens=1,2" %%A in ('call "%ADB_EXE%" devices 2^>nul ^| findstr /b /c:"!WIFI_SERIAL!"') do set "WIFI_STATE=%%B"
if /i "!WIFI_STATE!"=="device" (
    cls
    echo ================================================================
    echo ANDROID WIFI DEBUGGER - MONITORING
    echo ================================================================
    echo Device: !WIFI_SERIAL!
    echo Status: CONNECTED
    echo You may Run or Debug from Android Studio.
    echo.
    echo Press M for menu, R to reconnect, or E to exit.
    choice /c MREK /n /t 5 /d K >nul
    if errorlevel 4 goto :monitor_loop
    if errorlevel 3 exit /b 0
    if errorlevel 2 call "%SCRIPT_DIR%\reconnect.bat" reconnect
    if errorlevel 1 (
        call "%SCRIPT_DIR%\menu.bat" show
        if errorlevel 10 exit /b 0
    )
) else (
    cls
    echo Connection lost. Automatic reconnect is running...
    call "%SCRIPT_DIR%\logger.bat" WARN "Monitor detected a lost connection"
    call "%SCRIPT_DIR%\reconnect.bat" reconnect
    if errorlevel 1 call "%SCRIPT_DIR%\utils.bat" sleep 5
)
goto :monitor_loop
