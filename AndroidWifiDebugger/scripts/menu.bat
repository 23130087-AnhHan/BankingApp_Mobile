@echo off
if /i "%~1"=="show" goto :show
exit /b 2

:show
:menu_loop
cls
echo ===================================
echo.
echo   1  Connect
echo   2  Reconnect
echo   3  Restart ADB Server
echo   4  Disconnect
echo   5  Show Devices
echo   6  Show Phone IP
echo   7  Monitor
echo   8  Exit
echo.
echo ===================================
choice /c 12345678 /n /m "Select: "
if errorlevel 8 exit /b 10
if errorlevel 7 exit /b 0
if errorlevel 6 (
    echo.
    echo Phone IP: !PHONE_IP!
    pause
    goto :menu_loop
)
if errorlevel 5 (
    call "%SCRIPT_DIR%\adb.bat" show_devices
    pause
    goto :menu_loop
)
if errorlevel 4 (
    call "%SCRIPT_DIR%\reconnect.bat" disconnect
    pause
    goto :menu_loop
)
if errorlevel 3 (
    call "%SCRIPT_DIR%\adb.bat" restart_server
    pause
    goto :menu_loop
)
if errorlevel 2 (
    call "%SCRIPT_DIR%\reconnect.bat" reconnect
    pause
    goto :menu_loop
)
if errorlevel 1 (
    if not defined USB_SERIAL call "%SCRIPT_DIR%\adb.bat" wait_for_usb
    call "%SCRIPT_DIR%\adb.bat" enable_tcpip
    if not errorlevel 1 call "%SCRIPT_DIR%\network.bat" get_phone_ip "!USB_SERIAL!"
    if not errorlevel 1 call "%SCRIPT_DIR%\reconnect.bat" connect_current
    pause
    goto :menu_loop
)
goto :menu_loop

