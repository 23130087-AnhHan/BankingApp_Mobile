@echo off
setlocal enabledelayedexpansion
title KHOI CHAY TOAN BO HE THONG BANKING MICROSERVICES

echo ==============================================================
echo        KHOI CHAY HE THONG BANKING MICROSERVICES
echo ==============================================================
echo.

:: 1. Kiem tra JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo [CANH BAO] Bien moi truong JAVA_HOME chua duoc thiet lap.
    echo Thu lay java tu system PATH...
    java -version >nul 2>&1
    if errorlevel 1 (
        echo [LOI] Khong tim thay Java. Vui long cai dat JDK 17 va thiet lap JAVA_HOME.
        pause
        exit /b 1
    )
) else (
    echo [OK] Su dung JAVA_HOME: %JAVA_HOME%
)

:: 2. Nhap Keycloak Client Secret (mac dinh: TjeQGZma15XGmLGIMI6M84oBU9549Sf9)
echo.
echo De chay User-Service, he thong can Keycloak Client Secret.
echo (Ban co the lay tu Keycloak Admin Console > Clients > banking-service-api-client > Credentials)
set /p KEYCLOAK_CLIENT_SECRET="Nhap KEYCLOAK_CLIENT_SECRET [Nhan Enter de dung mac dinh: TjeQGZma15XGmLGIMI6M84oBU9549Sf9]: "
if "%KEYCLOAK_CLIENT_SECRET%"=="" (
    set "KEYCLOAK_CLIENT_SECRET=TjeQGZma15XGmLGIMI6M84oBU9549Sf9"
)
set "KEYCLOAK_URL=http://localhost:8571"
set "KEYCLOAK_REALM=banking-service"
set "KEYCLOAK_CLIENT_ID=banking-service-api-client"

echo.
echo --- KHOI CHAY CAC DICH VU MICROSERVICES ---

:: Eureka (Service Registry) - Run first and wait
echo [1/7] Dang khoi chay Service Registry (Eureka) tai port 8761...
start "EUREKA-SERVER" cmd /k "cd /d %~dp0Service-Registry && ..\mvnw.cmd spring-boot:run"
echo Doi Eureka Server khoi dong trong 15 giay...
timeout /t 15

:: API Gateway
echo [2/7] Dang khoi chay API Gateway tai port 8080...
start "API-GATEWAY" cmd /k "cd /d %~dp0API-Gateway && ..\mvnw.cmd spring-boot:run"
timeout /t 5

:: User-Service
echo [3/7] Dang khoi chay User Service tai port 8082...
start "USER-SERVICE" cmd /k "set KEYCLOAK_URL=%KEYCLOAK_URL%&& set KEYCLOAK_REALM=%KEYCLOAK_REALM%&& set KEYCLOAK_CLIENT_ID=%KEYCLOAK_CLIENT_ID%&& set KEYCLOAK_CLIENT_SECRET=%KEYCLOAK_CLIENT_SECRET%&& cd /d %~dp0User-Service && ..\mvnw.cmd spring-boot:run"

:: Sequence Generator
echo [4/7] Dang khoi chay Sequence Generator tai port 8083...
start "SEQUENCE-GENERATOR" cmd /k "cd /d %~dp0Sequence-Generator && ..\mvnw.cmd spring-boot:run"

:: Account-Service
echo [5/7] Dang khoi chay Account Service tai port 8081...
start "ACCOUNT-SERVICE" cmd /k "cd /d %~dp0Account-Service && ..\mvnw.cmd spring-boot:run"

:: Transaction-Service
echo [6/7] Dang khoi chay Transaction Service tai port 8084...
start "TRANSACTION-SERVICE" cmd /k "cd /d %~dp0Transaction-Service && ..\mvnw.cmd spring-boot:run"

:: Fund-Transfer
echo [7/7] Dang khoi chay Fund Transfer tai port 8085...
start "FUND-TRANSFER" cmd /k "cd /d %~dp0Fund-Transfer && ..\mvnw.cmd spring-boot:run"

echo.
echo ==============================================================
echo   TAT CA CAC CONG VIEC DA DUOC THUC HIEN!
echo ==============================================================
echo Eureka Dashboard: http://localhost:8761
echo API Gateway: http://localhost:8080
echo.
echo Luu y: Dam bao MySQL va Keycloak da duoc khoi dong truoc khi kiem tra.
echo ==============================================================
pause
