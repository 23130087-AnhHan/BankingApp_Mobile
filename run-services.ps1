# PowerShell Script to run ALL Microservices including Keycloak

$javaHome = "C:\Users\Asus\Desktop\jdk-17.0.12_windows-x64_bin\jdk-17.0.12"
$mavenPath = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd"
$kcPath = "C:\Tools\keycloak\keycloak-26.6.4\bin"

Write-Host "--- Starting Full Banking Application ---" -ForegroundColor Cyan

# 1. Start Keycloak
Write-Host "[1/4] Starting Keycloak..." -ForegroundColor Green
Start-Process powershell -ArgumentList "cd '$kcPath'; `$env:JAVA_HOME='$javaHome'; .\kc.bat start-dev --http-port 8571"
Start-Sleep -Seconds 15

# 2. Start Service Registry
Write-Host "[2/4] Starting Service Registry..." -ForegroundColor Green
cd Service-Registry
Start-Process powershell -ArgumentList "cd '$PWD'; `$env:JAVA_HOME='$javaHome'; & '$mavenPath' spring-boot:run"
Start-Sleep -Seconds 20

# 3. Start API Gateway
Write-Host "[3/4] Starting API Gateway..." -ForegroundColor Green
cd ..\API-Gateway
Start-Process powershell -ArgumentList "cd '$PWD'; `$env:JAVA_HOME='$javaHome'; & '$mavenPath' spring-boot:run"
Start-Sleep -Seconds 10

# 4. Start all other Microservices
Write-Host "[4/4] Starting Business Microservices..." -ForegroundColor Green
$services = @("Account-Service", "User-Service", "Transaction-Service", "Fund-Transfer", "Sequence-Generator")

foreach ($s in $services) {
    cd "..\$s"
    Write-Host "  -> Launching $s..." -ForegroundColor Gray
    Start-Process powershell -ArgumentList "cd '$PWD'; `$env:JAVA_HOME='$javaHome'; & '$mavenPath' spring-boot:run"
    Start-Sleep -Seconds 3
}

Write-Host "`nEverything launched! Check dashboard: http://localhost:8761" -ForegroundColor Cyan
