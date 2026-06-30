@echo off
:: Thiet lap moi truong
set "JAVA_HOME=C:\Users\Asus\Desktop\jdk-17.0.12_windows-x64_bin\jdk-17.0.12"
set "MAVEN_BIN=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd"
set "KC_BIN=C:\Tools\keycloak\keycloak-26.6.4\bin\kc.bat"

echo --- KHOI CHAY TOAN BO HE THONG BANKING ---

:: 1. Keycloak
echo [1/4] Dang mo Keycloak (Port 8571)...
start "KEYCLOAK" cmd /c "cd /d C:\Tools\keycloak\keycloak-26.6.4\bin && kc.bat start-dev --http-port 8571"
timeout /t 10

:: 2. Service Registry
echo [2/4] Dang mo Service Registry (Port 8761)...
start "EUREKA" cmd /c "cd /d %~dp0Service-Registry && "%MAVEN_BIN%" spring-boot:run"
timeout /t 15

:: 3. API Gateway
echo [3/4] Dang mo API Gateway (Port 8080)...
start "GATEWAY" cmd /c "cd /d %~dp0API-Gateway && "%MAVEN_BIN%" spring-boot:run"
timeout /t 5

:: 4. Business Services
echo [4/4] Dang mo cac service con lai...
start "USER" cmd /c "cd /d %~dp0User-Service && "%MAVEN_BIN%" spring-boot:run"
start "ACCOUNT" cmd /c "cd /d %~dp0Account-Service && "%MAVEN_BIN%" spring-boot:run"
start "TRANSACTION" cmd /c "cd /d %~dp0Transaction-Service && "%MAVEN_BIN%" spring-boot:run"
start "FUND-TRANSFER" cmd /c "cd /d %~dp0Fund-Transfer && "%MAVEN_BIN%" spring-boot:run"
start "SEQUENCE" cmd /c "cd /d %~dp0Sequence-Generator && "%MAVEN_BIN%" spring-boot:run"

echo.
echo === TAT CA DA DUOC KICH HOAT ===
echo Hay kiem tra Eureka tai: http://localhost:8761
pause
