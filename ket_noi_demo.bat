@echo off
title Ket Noi Demo Tu Dong - NLU Banking
color 0A
cls
echo ==============================================================
echo        TU DONG KET NOI DIEN THOAI DEMO - NLU BANKING
echo ==============================================================
echo.

set ADB_PATH="C:\Users\Asus\AppData\Local\Android\Sdk\platform-tools\adb.exe"

:: 1. Kiểm tra xem đã có thiết bị nào kết nối sẵn chưa (qua USB hoặc Wi-Fi)
echo 1. Dang kiem tra cac thiet bi dang ket noi san...
%ADB_PATH% devices | findstr /r /c:"[a-zA-Z0-9].*device$" >nul
if %errorlevel% equ 0 (
    echo [OK] Phat hien thiet bi da san sang!
    goto setup_reverse
)

:: 2. Nếu chưa có kết nối, thử dùng IP cũ gần nhất (mặc định là 192.168.1.13)
set LAST_IP=192.168.1.13
if exist "%temp%\nlu_last_ip.txt" (
    set /p LAST_IP=<"%temp%\nlu_last_ip.txt"
)

echo [Goi y] Dang thu ket noi khong day den IP truoc do (%LAST_IP%)...
%ADB_PATH% connect %LAST_IP%:5555 >nul 2>&1
timeout /t 2 >nul

%ADB_PATH% devices | findstr /r /c:"[a-zA-Z0-9].*device$" >nul
if %errorlevel% equ 0 (
    echo [OK] Da ket noi khong day thanh cong den %LAST_IP%!
    goto setup_reverse
)

:: 3. Nếu vẫn thất bại, yêu cầu cắm cáp USB để tự động lấy IP mới
cls
echo ==============================================================
echo  [CHU Y] CHUA KET NOI DUOC DIEN THOAI (DOI NOI PHONG DEMO)
echo ==============================================================
echo.
echo Vui long thuc hien cac buoc sau:
echo 1. Cam day cap USB ket noi tu dien thoai vao may tinh.
echo 2. Cho phep "Go loi USB" (USB Debugging) tren dien thoai neu co yeu cau.
echo.
echo Bam phim bat ky sau khi da cam day cap...
pause >nul

echo.
echo Dang kiem tra thiet bi USB...
%ADB_PATH% devices | findstr /r /c:"[a-zA-Z0-9].*device$" >nul
if %errorlevel% neq 0 (
    echo [LOI] Van chua nhan dang duoc thiet bi USB. Vui long kiem tra lai cap cam!
    pause
    exit
)

:: Tự động lấy IP của điện thoại trong mạng Wi-Fi mới
echo Dang tu dong quet IP cua dien thoai trong mang Wi-Fi hien tai...
set NEW_IP=
for /f "usebackq tokens=1-12" %%a in (`%ADB_PATH% shell ip route ^| findstr "wlan0"`) do (
    if "%%a"=="src" set NEW_IP=%%b
    if "%%b"=="src" set NEW_IP=%%c
    if "%%c"=="src" set NEW_IP=%%d
    if "%%d"=="src" set NEW_IP=%%e
    if "%%e"=="src" set NEW_IP=%%f
    if "%%f"=="src" set NEW_IP=%%g
    if "%%g"=="src" set NEW_IP=%%h
    if "%%h"=="src" set NEW_IP=%%i
    if "%%i"=="src" set NEW_IP=%%j
    if "%%j"=="src" set NEW_IP=%%k
    if "%%k"=="src" set NEW_IP=%%l
)

if "%NEW_IP%"=="" (
    echo [LOI] Khong the lay duoc IP Wi-Fi cua dien thoai. Dien thoai da bat Wi-Fi chua?
    pause
    exit
)

echo Phat hien IP dien thoai: %NEW_IP%
echo %NEW_IP%>"%temp%\nlu_last_ip.txt"

echo Dang kich hoat che do khong day (port 5555)...
%ADB_PATH% tcpip 5555 >nul 2>&1
timeout /t 2 >nul

echo.
echo [OK] DA THIET LAP XONG!
echo.
echo Bay gio ban hay:
echo 1. RUT DAY CAP USB ra khoi dien thoai.
echo 2. Bam phim bat ky de ket noi khong day qua Wi-Fi.
echo.
pause >nul

echo Dang thuc hien ket noi khong day toi %NEW_IP%...
%ADB_PATH% connect %NEW_IP%:5555
timeout /t 2 >nul

:setup_reverse
echo.
echo 3. Dang thiet lap chuyen huong cong (adb reverse)...
%ADB_PATH% reverse tcp:8080 tcp:8080
%ADB_PATH% reverse tcp:8082 tcp:8082

echo.
echo ==============================================================
echo THIET LAP THANH CONG! Danh sach thiet bi dang hoat dong:
echo ==============================================================
%ADB_PATH% devices
echo ==============================================================
echo.
pause
