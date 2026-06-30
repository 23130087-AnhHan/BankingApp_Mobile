@echo off
:: Thiet lap moi truong
set "JAVA_HOME=C:\Users\Asus\Desktop\jdk-17.0.12_windows-x64_bin\jdk-17.0.12"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_OPTS=-Xmx512m"

:: Set MySQL Environment Variables
set "MYSQL_USER=root"
set "MYSQL_PASSWORD=123456"
set "MYSQL_HOST=localhost"
set "MYSQL_PORT=3306"

echo --- KHOI CHAY TOAN BO HE THONG BANKING ---

:: 1. Keycloak
echo [1/4] Dang mo Keycloak (Port 8571)...
if exist "C:\Tools\keycloak\keycloak-26.6.4\bin\kc.bat" (
    start "KEYCLOAK" cmd /k "cd /d C:\Tools\keycloak\keycloak-26.6.4\bin && kc.bat start-dev --http-port=8571"
) else (
    echo [CANH BAO] Khong tim thay Keycloak tai C:\Tools\keycloak. Bo qua...
)
timeout /t 15

:: 2. Service Registry
echo [2/4] Dang mo Service Registry (Port 8761)...
start "EUREKA" cmd /k "cd /d %~dp0Service-Registry && ..\mvnw.cmd spring-boot:run"
timeout /t 30

:: 3. API Gateway
echo [3/4] Dang mo API Gateway (Port 8080)...
start "GATEWAY" cmd /k "cd /d %~dp0API-Gateway && ..\mvnw.cmd spring-boot:run"
timeout /t 15

:: 4. Business Services
echo [4/4] Dang mo cac service con lai...
start "USER" cmd /k "cd /d %~dp0User-Service && ..\mvnw.cmd spring-boot:run"
start "ACCOUNT" cmd /k "cd /d %~dp0Account-Service && ..\mvnw.cmd spring-boot:run"
start "TRANSACTION" cmd /k "cd /d %~dp0Transaction-Service && ..\mvnw.cmd spring-boot:run"
start "FUND-TRANSFER" cmd /k "cd /d %~dp0Fund-Transfer && ..\mvnw.cmd spring-boot:run"
start "SEQUENCE" cmd /k "cd /d %~dp0Sequence-Generator && ..\mvnw.cmd spring-boot:run"

echo.
echo === TAT CA LENH KHOI CHAY DA DUOC GUI ===
echo Neu cua so CMD nao tu dong dong, hay chay lenh '..\mvnw.cmd compile' trong thu muc do de kiem tra loi.
echo Kiem tra Eureka tai: http://localhost:8761
pause
