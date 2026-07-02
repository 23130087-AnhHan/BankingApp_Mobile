@echo off
if /i "%~1"=="check_environment" goto :check_environment
if /i "%~1"=="sleep" goto :sleep
if /i "%~1"=="dashboard" goto :dashboard
exit /b 2

:check_environment
ver | findstr /r /c:"Windows \[Version 10\." >nul || (
    echo ERROR: Windows 10 or Windows 11 is required.
    call "%SCRIPT_DIR%\logger.bat" ERROR "Unsupported Windows version"
    exit /b 1
)
for %%C in (powershell.exe ipconfig.exe netsh.exe) do (
    where %%C >nul 2>&1 || (
        echo ERROR: Required Windows command %%C was not found.
        call "%SCRIPT_DIR%\logger.bat" ERROR "Missing command %%C"
        exit /b 1
    )
)
call "%SCRIPT_DIR%\logger.bat" INFO "Environment check passed"
exit /b 0

:sleep
set "WAIT_SECONDS=%~2"
if not defined WAIT_SECONDS set "WAIT_SECONDS=1"
>nul 2>&1 timeout /t %WAIT_SECONDS% /nobreak
exit /b 0

:dashboard
cls
echo ================================================================
echo.
echo                    ANDROID WIFI DEBUGGER
echo.
echo ================================================================
echo.
echo   ADB Server          OK
if defined USB_SERIAL (echo   USB Device          OK  [!USB_SERIAL!]) else (echo   USB Device          NOT REQUIRED)
echo   Authorization       OK
echo   USB Debugging       OK
echo   TCP Mode            OK
echo   Phone IP            !PHONE_IP!
echo   ADB WiFi            CONNECTED  [!WIFI_SERIAL!]
echo.
echo ================================================================
echo                 READY TO UNPLUG USB
echo ================================================================
echo.
exit /b 0

