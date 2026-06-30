@echo off
setlocal EnableExtensions

set "PROJECT_ROOT=%~dp0"
set "JAVA_HOME=C:\Users\Asus\.jdks\ms-17.0.19"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Current stable values from the UI branch
set "MYSQL_USER=root"
set "MYSQL_PASSWORD=123456"
set "MYSQL_HOST=localhost"
set "MYSQL_PORT=3306"
set "KEYCLOAK_CLIENT_SECRET=932OFITe8vtH4z0G9gem4Afd69JhbwrI"

echo --------------------------------------------------
echo   Banking App Backend Runner
echo --------------------------------------------------
echo Project Root : %PROJECT_ROOT%
echo JAVA_HOME    : %JAVA_HOME%
echo.
echo Service-Registry is already running on 8761, so this script skips it.
echo.

if not exist "%PROJECT_ROOT%Sequence-Generator\pom.xml" goto :missing
start "Sequence-Generator" cmd /k "cd /d ""%PROJECT_ROOT%Sequence-Generator"" && set MYSQL_USER=%MYSQL_USER% && set MYSQL_PASSWORD=%MYSQL_PASSWORD% && set MYSQL_HOST=%MYSQL_HOST% && set MYSQL_PORT=%MYSQL_PORT% && mvn -f pom.xml spring-boot:run"
timeout /t 5 /nobreak >nul

if not exist "%PROJECT_ROOT%Account-Service\pom.xml" goto :missing
start "Account-Service" cmd /k "cd /d ""%PROJECT_ROOT%Account-Service"" && set MYSQL_USER=%MYSQL_USER% && set MYSQL_PASSWORD=%MYSQL_PASSWORD% && set MYSQL_HOST=%MYSQL_HOST% && set MYSQL_PORT=%MYSQL_PORT% && mvn -f pom.xml spring-boot:run"
timeout /t 5 /nobreak >nul

if not exist "%PROJECT_ROOT%Transaction-Service\pom.xml" goto :missing
start "Transaction-Service" cmd /k "cd /d ""%PROJECT_ROOT%Transaction-Service"" && set MYSQL_USER=%MYSQL_USER% && set MYSQL_PASSWORD=%MYSQL_PASSWORD% && set MYSQL_HOST=%MYSQL_HOST% && set MYSQL_PORT=%MYSQL_PORT% && mvn -f pom.xml spring-boot:run"
timeout /t 5 /nobreak >nul

if not exist "%PROJECT_ROOT%Fund-Transfer\pom.xml" goto :missing
start "Fund-Transfer" cmd /k "cd /d ""%PROJECT_ROOT%Fund-Transfer"" && set MYSQL_USER=%MYSQL_USER% && set MYSQL_PASSWORD=%MYSQL_PASSWORD% && set MYSQL_HOST=%MYSQL_HOST% && set MYSQL_PORT=%MYSQL_PORT% && mvn -f pom.xml spring-boot:run"
timeout /t 5 /nobreak >nul

if not exist "%PROJECT_ROOT%API-Gateway\pom.xml" goto :missing
start "API-Gateway" cmd /k "cd /d ""%PROJECT_ROOT%API-Gateway"" && mvn -f pom.xml spring-boot:run"

echo.
echo Commands sent. Refresh Eureka after a few seconds:
echo http://localhost:8761
echo Press any key to close...
pause >nul
exit /b 0

:missing
echo [ERROR] A required service folder or pom.xml was not found.
pause >nul
exit /b 1
