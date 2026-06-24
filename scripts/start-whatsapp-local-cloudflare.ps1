param(
    [string]$VerifyToken = "chatbot-contabilidade-local",
    [string]$CloudflaredPath = ".\tools\cloudflared\cloudflared.exe",
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

function Import-DotEnv {
    param([string]$Path = ".env")

    if (-not (Test-Path $Path)) {
        return
    }

    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#") -or -not $line.Contains("=")) {
            return
        }

        $parts = $line.Split("=", 2)
        $name = $parts[0].Trim()
        $value = $parts[1].Trim().Trim('"')
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

function Stop-PortProcess {
    param([int]$LocalPort)

    $processId = Get-NetTCPConnection -LocalPort $LocalPort -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty OwningProcess

    if ($processId) {
        Stop-Process -Id $processId -Force
        Start-Sleep -Seconds 2
    }
}

Import-DotEnv

if (-not $env:WHATSAPP_CLOUD_PHONE_NUMBER_ID) {
    throw "Configure WHATSAPP_CLOUD_PHONE_NUMBER_ID no arquivo .env."
}

if (-not $env:WHATSAPP_CLOUD_ACCESS_TOKEN) {
    throw "Configure WHATSAPP_CLOUD_ACCESS_TOKEN no arquivo .env."
}

if (-not (Test-Path $CloudflaredPath)) {
    throw "cloudflared nao encontrado em $CloudflaredPath."
}

$env:PATH = "C:\Windows\System32\WindowsPowerShell\v1.0;C:\Windows\System32;C:\Windows;C:\Program Files\Maven\bin;C:\Program Files\Java\jdk-17\bin;" + $env:PATH
$env:SPRING_DATASOURCE_URL = "jdbc:h2:mem:chatbot;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
$env:SPRING_DATASOURCE_USERNAME = "sa"
$env:SPRING_DATASOURCE_PASSWORD = ""
$env:SPRING_FLYWAY_ENABLED = "false"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = "create-drop"
$env:WHATSAPP_CLOUD_ENABLED = "true"
$env:WHATSAPP_CLOUD_API_VERSION = if ($env:WHATSAPP_CLOUD_API_VERSION) { $env:WHATSAPP_CLOUD_API_VERSION } else { "v23.0" }
$env:WHATSAPP_CLOUD_VERIFY_TOKEN = $VerifyToken

Stop-PortProcess -LocalPort $Port
Get-Process cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force

$backendArgs = "-NoProfile -ExecutionPolicy Bypass -Command `"Set-Location -LiteralPath '$PWD'; mvn -gs .mvn/settings.xml -s .mvn/settings.xml '-Dspring-boot.run.useTestClasspath=true' spring-boot:run *> app-local.out.log`""
$backend = Start-Process powershell.exe -ArgumentList $backendArgs -WindowStyle Hidden -PassThru

Write-Host "Backend iniciado. PID: $($backend.Id)"
Write-Host "Aguardando http://localhost:$Port ..."

$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$Port/api/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=$VerifyToken&hub.challenge=123456" -UseBasicParsing -TimeoutSec 3
        if ($response.StatusCode -eq 200 -and $response.Content -eq "123456") {
            $ready = $true
            break
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $ready) {
    throw "Backend nao respondeu ao webhook local. Veja app-local.out.log."
}

$cloudflaredExe = (Resolve-Path $CloudflaredPath).Path
$cloudflaredCommand = "Set-Location -LiteralPath '$PWD'; & '$cloudflaredExe' tunnel --protocol http2 --url http://localhost:$Port *> cloudflared.err.log"
$cloudflaredArgs = "-NoProfile -ExecutionPolicy Bypass -Command `"$cloudflaredCommand`""
$cloudflared = Start-Process "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" -ArgumentList $cloudflaredArgs -WindowStyle Hidden -PassThru

Write-Host "Cloudflare Tunnel iniciado. PID: $($cloudflared.Id)"
Write-Host "Aguardando URL publica..."

$publicUrl = $null
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    if (Test-Path cloudflared.err.log) {
        $publicUrl = Select-String -Path cloudflared.err.log -Pattern "https://(?!api\.)[-a-z0-9]+\.trycloudflare\.com" |
            Select-Object -Last 1 |
            ForEach-Object { $_.Matches[0].Value }
    }

    if ($publicUrl) {
        break
    }
}

if (-not $publicUrl) {
    throw "Nao foi possivel encontrar a URL publica. Veja cloudflared.err.log."
}

Write-Host ""
Write-Host "Configure na Meta:"
Write-Host "Callback URL: $publicUrl/api/whatsapp/webhook"
Write-Host "Verify token: $VerifyToken"
Write-Host "Campo para assinar: messages"
Write-Host ""
Write-Host "Painel local: http://localhost:$Port"
