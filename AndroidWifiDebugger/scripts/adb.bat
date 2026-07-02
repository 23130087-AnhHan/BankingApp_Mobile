@echo off
if /i "%~1"=="find_adb" goto :find_adb
if /i "%~1"=="restart_server" goto :restart_server
if /i "%~1"=="wait_for_usb" goto :wait_for_usb
if /i "%~1"=="enable_tcpip" goto :enable_tcpip
if /i "%~1"=="try_paired_wireless" goto :try_paired_wireless
if /i "%~1"=="show_devices" goto :show_devices
exit /b 2

:find_adb
set "ADB_EXE="
for %%P in (
    "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    "%ANDROID_SDK_ROOT%\platform-tools\adb.exe"
    "%ANDROID_HOME%\platform-tools\adb.exe"
    "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe"
) do if not defined ADB_EXE if exist "%%~P" set "ADB_EXE=%%~fP"
if not defined ADB_EXE for /f "delims=" %%P in ('where adb.exe 2^>nul') do if not defined ADB_EXE set "ADB_EXE=%%~fP"
if not defined ADB_EXE (
    echo ERROR: adb.exe was not found. Install Android SDK Platform-Tools.
    call "%SCRIPT_DIR%\logger.bat" ERROR "adb.exe not found"
    exit /b 1
)
echo ADB found: !ADB_EXE!
call "%SCRIPT_DIR%\logger.bat" INFO "ADB found at !ADB_EXE!"
exit /b 0

:restart_server
echo Restarting ADB server...
"%ADB_EXE%" kill-server >nul 2>&1
"%ADB_EXE%" start-server >nul 2>&1 || (
    echo ERROR: ADB server could not start.
    call "%SCRIPT_DIR%\logger.bat" ERROR "ADB server start failed"
    exit /b 1
)
call "%SCRIPT_DIR%\logger.bat" INFO "ADB server restarted"
exit /b 0

:wait_for_usb
set "USB_SERIAL="
:wait_usb_loop
set "FOUND_UNAUTHORIZED="
set "FOUND_OFFLINE="
for /f "skip=1 tokens=1,2" %%A in ('call "%ADB_EXE%" devices 2^>nul') do (
    set "IS_PHYSICAL_USB=1"
    echo %%A | findstr ":" >nul && set "IS_PHYSICAL_USB="
    echo %%A | findstr /i /b /c:"emulator-" >nul && set "IS_PHYSICAL_USB="
    if defined IS_PHYSICAL_USB (
        if /i "%%B"=="device" if not defined USB_SERIAL set "USB_SERIAL=%%A"
        if /i "%%B"=="unauthorized" set "FOUND_UNAUTHORIZED=1"
        if /i "%%B"=="offline" set "FOUND_OFFLINE=1"
    )
)
if defined USB_SERIAL (
    echo USB device detected: !USB_SERIAL!
    call "%SCRIPT_DIR%\logger.bat" INFO "Authorized USB device detected"
    exit /b 0
)
if defined FOUND_UNAUTHORIZED (
    cls
    echo Please press "Allow USB Debugging" on your phone.
    echo Keep the phone unlocked and select "Always allow from this computer".
) else (
    echo Waiting for USB device...
)
call "%SCRIPT_DIR%\utils.bat" sleep 2
goto :wait_usb_loop

:enable_tcpip
for /l %%R in (1,1,5) do (
    echo Enabling ADB TCP/IP mode - attempt %%R of 5...
    set "TCP_RESULT="
    for /f "delims=" %%L in ('call "%ADB_EXE%" -s "%USB_SERIAL%" tcpip %ADB_PORT% 2^>^&1') do set "TCP_RESULT=!TCP_RESULT! %%L"
    echo !TCP_RESULT! | findstr /i "restarting in TCP mode" >nul && (
        call "%SCRIPT_DIR%\logger.bat" INFO "ADB TCP/IP mode enabled on port %ADB_PORT%"
        exit /b 0
    )
    call "%SCRIPT_DIR%\logger.bat" WARN "TCP/IP attempt %%R failed: !TCP_RESULT!"
    call "%SCRIPT_DIR%\utils.bat" sleep 2
)
for /f "delims=" %%V in ('call "%ADB_EXE%" -s "%USB_SERIAL%" shell getprop ro.build.version.sdk 2^>nul') do set "ANDROID_SDK=%%V"
echo ERROR: Could not switch the phone to TCP/IP mode.
if defined ANDROID_SDK if !ANDROID_SDK! GEQ 30 (
    echo Android 11 or newer detected. Open Developer options ^> Wireless debugging.
    echo Pair once with "Pair device with pairing code", then run Demo.bat again.
)
call "%SCRIPT_DIR%\logger.bat" ERROR "Unable to enable TCP/IP mode"
exit /b 1

:try_paired_wireless
set "WIFI_SERIAL="
for /f "skip=1 tokens=1,2" %%A in ('call "%ADB_EXE%" devices 2^>nul') do (
    echo %%A | findstr ":" >nul && if /i "%%B"=="device" set "WIFI_SERIAL=%%A"
)
if defined WIFI_SERIAL (
    for /f "tokens=1 delims=:" %%I in ("!WIFI_SERIAL!") do set "PHONE_IP=%%I"
    call "%SCRIPT_DIR%\logger.bat" INFO "Reused existing wireless ADB connection"
    exit /b 0
)
rem A previously paired Android 11+ endpoint may be advertised through mDNS.
for /f "tokens=1,2" %%A in ('call "%ADB_EXE%" mdns services 2^>nul ^| findstr "_adb-tls-connect._tcp"') do (
    if not defined WIFI_SERIAL (
        "%ADB_EXE%" connect %%B >nul 2>&1
        for /f "tokens=1,2" %%D in ('call "%ADB_EXE%" devices 2^>nul ^| findstr /b /c:"%%B"') do if /i "%%E"=="device" set "WIFI_SERIAL=%%D"
    )
)
if defined WIFI_SERIAL (
    for /f "tokens=1 delims=:" %%I in ("!WIFI_SERIAL!") do set "PHONE_IP=%%I"
    call "%SCRIPT_DIR%\logger.bat" INFO "Connected through paired Wireless Debugging"
)
exit /b 0

:show_devices
echo.
"%ADB_EXE%" devices -l
echo.
exit /b 0
