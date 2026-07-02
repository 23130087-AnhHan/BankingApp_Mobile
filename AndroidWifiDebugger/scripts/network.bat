@echo off
if /i "%~1"=="get_phone_ip" goto :get_phone_ip
if /i "%~1"=="discover_candidates" goto :discover_candidates
exit /b 2

:get_phone_ip
set "TARGET_SERIAL=%~2"
if not defined TARGET_SERIAL set "TARGET_SERIAL=%USB_SERIAL%"
set "PHONE_IP="

rem Android `ip -o -4 addr` prints the CIDR address as token four.
rem Prefer wlan0, then inspect every globally scoped interface.
for /f "tokens=4" %%I in ('call "%ADB_EXE%" -s "%TARGET_SERIAL%" shell ip -o -4 addr show wlan0 2^>nul') do (
    for /f "tokens=1 delims=/" %%J in ("%%I") do if not defined PHONE_IP set "PHONE_IP=%%J"
)
if not defined PHONE_IP for /f "tokens=4" %%I in ('call "%ADB_EXE%" -s "%TARGET_SERIAL%" shell ip -o -4 addr show scope global 2^>nul') do (
    for /f "tokens=1 delims=/" %%J in ("%%I") do (
        if not defined FALLBACK_IP set "FALLBACK_IP=%%J"
        for /f "tokens=1,2 delims=." %%A in ("%%J") do (
            if "%%A"=="10" if not defined PHONE_IP set "PHONE_IP=%%J"
            if "%%A"=="192" if "%%B"=="168" if not defined PHONE_IP set "PHONE_IP=%%J"
            if "%%A"=="172" if %%B GEQ 16 if %%B LEQ 31 if not defined PHONE_IP set "PHONE_IP=%%J"
        )
    )
)
if not defined PHONE_IP if defined FALLBACK_IP set "PHONE_IP=!FALLBACK_IP!"
set "FALLBACK_IP="

if not defined PHONE_IP (
    echo ERROR: No phone IPv4 address was found. Connect phone and PC to the same network.
    call "%SCRIPT_DIR%\logger.bat" ERROR "Phone IPv4 discovery failed"
    exit /b 1
)
powershell -NoProfile -Command "$ip='!PHONE_IP!'; $parsed=$null; if([System.Net.IPAddress]::TryParse($ip,[ref]$parsed) -and $ip -notlike '0.*' -and $ip -notlike '127.*'){exit 0}else{exit 1}" || (
    echo ERROR: Invalid phone IP detected: !PHONE_IP!
    call "%SCRIPT_DIR%\logger.bat" ERROR "Invalid phone IP !PHONE_IP!"
    set "PHONE_IP="
    exit /b 1
)
echo.
echo Phone IP
echo.
echo !PHONE_IP!
echo.
call "%SCRIPT_DIR%\logger.bat" INFO "Phone IP detected"
exit /b 0

:discover_candidates
set "CANDIDATE_FILE=%TEMP%\android-wifi-debugger-candidates.txt"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ips = Get-NetNeighbor -AddressFamily IPv4 -ErrorAction SilentlyContinue ^| Where-Object State -ne Unreachable ^| Select-Object -ExpandProperty IPAddress ^| Where-Object {$_ -match '^(10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.)'}; $ips ^| Sort-Object -Unique ^| Set-Content -Encoding ASCII '%CANDIDATE_FILE%'" >nul 2>&1
exit /b 0
