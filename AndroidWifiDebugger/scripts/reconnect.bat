@echo off
if /i "%~1"=="connect_current" goto :connect_current
if /i "%~1"=="reconnect" goto :reconnect
if /i "%~1"=="disconnect" goto :disconnect
exit /b 2

:connect_current
if not defined PHONE_IP exit /b 1
set "WIFI_SERIAL=%PHONE_IP%:%ADB_PORT%"
echo Connecting to !WIFI_SERIAL!...
"%ADB_EXE%" connect "!WIFI_SERIAL!" >nul 2>&1
call "%SCRIPT_DIR%\utils.bat" sleep 1
for /f "tokens=1,2" %%A in ('call "%ADB_EXE%" devices 2^>nul ^| findstr /b /c:"!WIFI_SERIAL!"') do set "WIFI_STATE=%%B"
if /i "!WIFI_STATE!"=="offline" (
    "%ADB_EXE%" disconnect "!WIFI_SERIAL!" >nul 2>&1
    call "%SCRIPT_DIR%\utils.bat" sleep 1
    "%ADB_EXE%" connect "!WIFI_SERIAL!" >nul 2>&1
    call "%SCRIPT_DIR%\utils.bat" sleep 1
    set "WIFI_STATE="
    for /f "tokens=1,2" %%A in ('call "%ADB_EXE%" devices 2^>nul ^| findstr /b /c:"!WIFI_SERIAL!"') do set "WIFI_STATE=%%B"
)
if /i not "!WIFI_STATE!"=="device" (
    echo ERROR: ADB could not connect to !WIFI_SERIAL!.
    call "%SCRIPT_DIR%\logger.bat" ERROR "WiFi connection failed"
    exit /b 1
)
echo ADB over WiFi Connected
call "%SCRIPT_DIR%\logger.bat" INFO "ADB WiFi connected"
exit /b 0

:reconnect
call "%SCRIPT_DIR%\logger.bat" WARN "Reconnect started"
if defined WIFI_SERIAL "%ADB_EXE%" disconnect "!WIFI_SERIAL!" >nul 2>&1

rem First try the last known address.
if defined PHONE_IP (
    call :connect_current
    if not errorlevel 1 exit /b 0
)

rem If USB is still attached, query the phone for its new address.
if defined USB_SERIAL (
    "%ADB_EXE%" -s "!USB_SERIAL!" get-state 2>nul | findstr /x "device" >nul && (
        call "%SCRIPT_DIR%\network.bat" get_phone_ip "!USB_SERIAL!"
        if not errorlevel 1 call :connect_current
        if not errorlevel 1 exit /b 0
    )
)

rem Try Android 11+ paired Wireless Debugging advertisements.
call "%SCRIPT_DIR%\adb.bat" try_paired_wireless
if defined WIFI_SERIAL exit /b 0

rem Classic TCP/IP does not advertise itself. Probe known LAN/hotspot neighbors.
call "%SCRIPT_DIR%\network.bat" discover_candidates
if exist "%CANDIDATE_FILE%" for /f "usebackq delims=" %%I in ("%CANDIDATE_FILE%") do (
    if not defined RECONNECT_FOUND (
        set "PHONE_IP=%%I"
        call :connect_current >nul 2>&1
        if not errorlevel 1 set "RECONNECT_FOUND=1"
    )
)
if defined RECONNECT_FOUND (
    set "RECONNECT_FOUND="
    call "%SCRIPT_DIR%\logger.bat" INFO "Phone rediscovered after network change"
    exit /b 0
)

echo Reconnect failed. Keep the phone unlocked and Wireless debugging enabled.
echo If the phone changed networks and TCP mode was reset, reconnect USB and run Connect.
call "%SCRIPT_DIR%\logger.bat" ERROR "Automatic reconnect exhausted all discovery methods"
exit /b 1

:disconnect
if defined WIFI_SERIAL "%ADB_EXE%" disconnect "!WIFI_SERIAL!" >nul 2>&1
call "%SCRIPT_DIR%\logger.bat" INFO "Manual disconnect"
set "WIFI_SERIAL="
exit /b 0
