param(
    [string]$JavaHome = "C:\Users\Asus\.jdks\ms-17.0.19"
)

# Keep the parent shell usable while each service runs in its own window.
$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot

if (-not (Test-Path $JavaHome)) {
    throw "JAVA_HOME not found: $JavaHome"
}

$mvnw = Join-Path $projectRoot "mvnw.cmd"
$mvnCmd = if (Test-Path $mvnw) { $mvnw } else { "mvn" }

function Start-ServiceWindow {
    param(
        [Parameter(Mandatory = $true)][string]$ServiceName,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory,
        [int]$WaitSeconds = 0
    )

    $command = @"
`$env:JAVA_HOME = '$JavaHome'
`$env:Path = "`$env:JAVA_HOME\bin;`$env:Path"
Set-Location '$WorkingDirectory'
Write-Host 'Starting $ServiceName...' -ForegroundColor Cyan
$mvnCmd -f .\pom.xml spring-boot:run
"@

    Start-Process powershell -WorkingDirectory $WorkingDirectory -ArgumentList @("-NoExit", "-Command", $command)

    if ($WaitSeconds -gt 0) {
        Start-Sleep -Seconds $WaitSeconds
    }
}

Write-Host "--- Starting Banking Application Microservices ---" -ForegroundColor Cyan
Write-Host "Using JAVA_HOME: $JavaHome" -ForegroundColor Yellow

# 1. Start Service Registry first so Eureka is available before other services register.
if (Test-Path (Join-Path $projectRoot "Service-Registry")) {
    Start-ServiceWindow -ServiceName "Service-Registry" -WorkingDirectory (Join-Path $projectRoot "Service-Registry") -WaitSeconds 15
} else {
    Write-Host "Service-Registry folder not found." -ForegroundColor Red
}

# 2. Start API Gateway after Eureka has time to come up.
if (Test-Path (Join-Path $projectRoot "API-Gateway")) {
    Start-ServiceWindow -ServiceName "API-Gateway" -WorkingDirectory (Join-Path $projectRoot "API-Gateway") -WaitSeconds 5
} else {
    Write-Host "API-Gateway folder not found." -ForegroundColor Red
}

# 3. Start the remaining business services.
$serviceOrder = @(
    "User-Service",
    "Sequence-Generator",
    "Account-Service",
    "Transaction-Service",
    "Fund-Transfer"
)

foreach ($service in $serviceOrder) {
    $servicePath = Join-Path $projectRoot $service
    if (Test-Path $servicePath) {
        Start-ServiceWindow -ServiceName $service -WorkingDirectory $servicePath -WaitSeconds 3
    } else {
        Write-Host "$service folder not found." -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "All services have been triggered in separate windows." -ForegroundColor Green
Write-Host "Before testing the app, make sure MySQL and Keycloak are running if your flow needs them." -ForegroundColor Magenta
