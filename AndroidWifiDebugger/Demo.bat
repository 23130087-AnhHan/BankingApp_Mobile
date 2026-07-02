@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Android WiFi Debugger
color 0A

set "APP_ROOT=%~dp0"
set "SCRIPT_DIR=%APP_ROOT%scripts"
set "LOG_DIR=%APP_ROOT%logs"
set "LOG_FILE=%LOG_DIR%\adb.log"
set "ADB_EXE="
set "USB_SERIAL="
set "WIFI_SERIAL="
set "PHONE_IP="
set "ADB_PORT=5555"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%" >nul 2>&1
call "%SCRIPT_DIR%\logger.bat" INFO "Application started"
call "%SCRIPT_DIR%\utils.bat" check_environment || goto :fatal
call "%SCRIPT_DIR%\adb.bat" find_adb || goto :fatal
call "%SCRIPT_DIR%\adb.bat" restart_server || goto :fatal

rem Prefer an already paired Android 11+ Wireless Debugging endpoint.
call "%SCRIPT_DIR%\adb.bat" try_paired_wireless
if defined WIFI_SERIAL goto :connected

call "%SCRIPT_DIR%\adb.bat" wait_for_usb || goto :fatal
call "%SCRIPT_DIR%\adb.bat" enable_tcpip || goto :fatal
call "%SCRIPT_DIR%\utils.bat" sleep 3
call "%SCRIPT_DIR%\network.bat" get_phone_ip "%USB_SERIAL%" || goto :fatal
call "%SCRIPT_DIR%\reconnect.bat" connect_current || goto :fatal

:connected
call "%SCRIPT_DIR%\utils.bat" dashboard
call "%SCRIPT_DIR%\logger.bat" INFO "READY TO UNPLUG USB; WiFi serial=%WIFI_SERIAL%"
echo.
echo Monitoring starts automatically. Press M in monitor mode to open the menu.
call "%SCRIPT_DIR%\utils.bat" sleep 3
call "%SCRIPT_DIR%\monitor.bat" run
goto :end

:fatal
echo.
echo ================================================================
echo ERROR: The operation could not be completed.
echo See: %LOG_FILE%
echo ================================================================
call "%SCRIPT_DIR%\logger.bat" ERROR "Application stopped because of a fatal error"
pause

:end
call "%SCRIPT_DIR%\logger.bat" INFO "Application exited"
endlocal

