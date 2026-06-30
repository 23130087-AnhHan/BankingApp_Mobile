param(
    [string]$JavaHome = "C:\Program Files\Java\jdk-17"
)

$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot
$javaExecutable = Join-Path $JavaHome "bin\java.exe"

# Keycloak runs from the Windows ZIP on localhost:8571; this script does not use Docker.
if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "Khong tim thay JDK 17 tai '$JavaHome'. Chay lai voi -JavaHome <duong-dan-jdk-17>."
}

$secret = Read-Host "Nhap KEYCLOAK_CLIENT_SECRET that tu Keycloak Admin Console (khong luu vao file)"
if ([string]::IsNullOrWhiteSpace($secret)) {
    throw "KEYCLOAK_CLIENT_SECRET khong duoc de trong. Script da dung va khong khoi dong User-Service."
}

$env:JAVA_HOME = $JavaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:KEYCLOAK_URL = "http://localhost:8571"
$env:KEYCLOAK_REALM = "banking-service"
$env:KEYCLOAK_CLIENT_ID = "banking-service-api-client"
$env:KEYCLOAK_CLIENT_SECRET = $secret

try {
    $wellKnownUrl = "$env:KEYCLOAK_URL/realms/$env:KEYCLOAK_REALM/.well-known/openid-configuration"
    try {
        Invoke-RestMethod -Uri $wellKnownUrl -Method Get -TimeoutSec 5 | Out-Null
    } catch {
        throw "Khong ket noi duoc Keycloak tai '$env:KEYCLOAK_URL'. Hay khoi dong Keycloak va kiem tra realm '$env:KEYCLOAK_REALM'."
    }

    Set-Location -LiteralPath $repoRoot
    & ".\mvnw.cmd" -f ".\User-Service\pom.xml" spring-boot:run
    if ($LASTEXITCODE -ne 0) {
        throw "User-Service dung voi exit code $LASTEXITCODE."
    }
} finally {
    $env:KEYCLOAK_CLIENT_SECRET = $null
    $secret = $null
}
