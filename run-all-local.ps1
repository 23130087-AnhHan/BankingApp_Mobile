param(
    [string]$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot",
    [string]$KeycloakHome = "C:\Tools\keycloak",
    [int]$StartupDelaySeconds = 4
)

$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot

function Test-Port {
    param(
        [string]$HostName,
        [int]$Port
    )

    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        $connected = $async.AsyncWaitHandle.WaitOne(800)
        if ($connected) {
            $client.EndConnect($async)
        }
        $client.Close()
        return $connected
    } catch {
        return $false
    }
}

function Wait-Port {
    param(
        [string]$Name,
        [int]$Port,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port -HostName "localhost" -Port $Port) {
            Write-Host "$Name is listening on port $Port."
            return
        }
        Start-Sleep -Seconds 2
    }
    Write-Warning "$Name did not start listening on port $Port within $TimeoutSeconds seconds. Continuing so you can inspect logs."
}

function Start-Terminal {
    param(
        [string]$Title,
        [string]$WorkingDirectory,
        [string]$Command
    )

    $fullCommand = @"
`$host.UI.RawUI.WindowTitle = '$Title'
Set-Location -LiteralPath '$WorkingDirectory'
`$env:JAVA_HOME = '$JavaHome'
`$env:Path = (Join-Path `$env:JAVA_HOME 'bin') + ';' + `$env:Path
`$env:MAVEN_OPTS = '-Xmx512m -XX:MaxMetaspaceSize=256m'
$Command
"@

    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($fullCommand))
    Start-Process powershell.exe -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-EncodedCommand", $encodedCommand
    ) -WorkingDirectory $WorkingDirectory
}

function Resolve-KeycloakHome {
    param([string]$KeycloakPath)

    if (Test-Path -LiteralPath (Join-Path $KeycloakPath "bin\kc.bat")) {
        return (Resolve-Path -LiteralPath $KeycloakPath).Path
    }

    $nested = Get-ChildItem -LiteralPath $KeycloakPath -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin\kc.bat") } |
        Select-Object -First 1

    if ($nested) {
        return $nested.FullName
    }

    throw "Cannot find Keycloak at '$KeycloakPath'. Expected bin\kc.bat under that folder."
}

if (-not (Test-Path -LiteralPath (Join-Path $repoRoot "mvnw.cmd"))) {
    throw "Cannot find mvnw.cmd in '$repoRoot'. Run this script from the repository root."
}

if (-not (Test-Path -LiteralPath (Join-Path $JavaHome "bin\java.exe"))) {
    throw "Cannot find JDK at '$JavaHome'. Pass -JavaHome with your JDK 17 path."
}

$secureSecret = Read-Host "Nhap KEYCLOAK_CLIENT_SECRET tu Keycloak Admin Console" -AsSecureString
$secretPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureSecret)
$clientSecret = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($secretPtr)
[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secretPtr)

if ([string]::IsNullOrWhiteSpace($clientSecret)) {
    throw "KEYCLOAK_CLIENT_SECRET khong duoc de trong."
}

$resolvedKeycloakHome = Resolve-KeycloakHome -KeycloakPath $KeycloakHome

if (-not (Test-Port -HostName "localhost" -Port 8571)) {
    Start-Terminal `
        -Title "Keycloak :8571" `
        -WorkingDirectory $resolvedKeycloakHome `
        -Command @"
`$env:KEYCLOAK_ADMIN = 'admin'
`$env:KEYCLOAK_ADMIN_PASSWORD = 'admin'
.\bin\kc.bat start-dev --http-port=8571
"@
    Wait-Port -Name "Keycloak" -Port 8571 -TimeoutSeconds 90
} else {
    Write-Host "Keycloak is already listening on port 8571."
}

if (-not (Test-Port -HostName "localhost" -Port 8761)) {
    Start-Terminal -Title "Service Registry :8761" -WorkingDirectory $repoRoot -Command ".\mvnw.cmd -f .\Service-Registry\pom.xml spring-boot:run"
    Wait-Port -Name "Service Registry" -Port 8761 -TimeoutSeconds 90
} else {
    Write-Host "Service Registry is already listening on port 8761."
}

$services = @(
    @{ Title = "Sequence Generator :8083"; Pom = ".\Sequence-Generator\pom.xml"; Port = 8083 },
    @{ Title = "Account Service :8081"; Pom = ".\Account-Service\pom.xml"; Port = 8081 },
    @{ Title = "Transaction Service :8084"; Pom = ".\Transaction-Service\pom.xml"; Port = 8084 },
    @{ Title = "Fund Transfer :8085"; Pom = ".\Fund-Transfer\pom.xml"; Port = 8085 }
)

foreach ($service in $services) {
    if (Test-Port -HostName "localhost" -Port $service.Port) {
        Write-Host "$($service.Title) is already listening."
        continue
    }
    Start-Terminal -Title $service.Title -WorkingDirectory $repoRoot -Command ".\mvnw.cmd -f $($service.Pom) spring-boot:run"
    Start-Sleep -Seconds $StartupDelaySeconds
}

if (-not (Test-Port -HostName "localhost" -Port 8082)) {
    $env:KEYCLOAK_URL = "http://localhost:8571"
    $env:KEYCLOAK_REALM = "banking-service"
    $env:KEYCLOAK_CLIENT_ID = "banking-service-api-client"
    $env:KEYCLOAK_CLIENT_SECRET = $clientSecret
    Start-Terminal -Title "User Service :8082" -WorkingDirectory $repoRoot -Command @"
`$env:KEYCLOAK_URL = 'http://localhost:8571'
`$env:KEYCLOAK_REALM = 'banking-service'
`$env:KEYCLOAK_CLIENT_ID = 'banking-service-api-client'
.\mvnw.cmd -f .\User-Service\pom.xml spring-boot:run
"@
    $env:KEYCLOAK_CLIENT_SECRET = $null
    $clientSecret = $null
} else {
    Write-Host "User Service is already listening on port 8082."
}

Start-Sleep -Seconds $StartupDelaySeconds
if (-not (Test-Port -HostName "localhost" -Port 8080)) {
    Start-Terminal -Title "API Gateway :8080" -WorkingDirectory $repoRoot -Command ".\mvnw.cmd -f .\API-Gateway\pom.xml spring-boot:run"
} else {
    Write-Host "API Gateway is already listening on port 8080."
}

Write-Host ""
Write-Host "Started local stack windows. Check Eureka after services finish booting:"
Write-Host "http://localhost:8761"
Write-Host ""
Write-Host "Expected apps: API-GATEWAY, USER-SERVICE, ACCOUNT-SERVICE, SEQUENCE-GENERATOR, TRANSACTION-SERVICE, FUND-TRANSFER."
