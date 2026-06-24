param(
    [Parameter(Mandatory = $true)]
    [string]$HostName,

    [Parameter(Mandatory = $false)]
    [string]$User = "opc",

    [Parameter(Mandatory = $false)]
    [string]$SshKeyPath = "",

    [Parameter(Mandatory = $true)]
    [string]$PhoneNumberId,

    [Parameter(Mandatory = $true)]
    [string]$AccessToken,

    [Parameter(Mandatory = $false)]
    [string]$VerifyToken = "chatbot-contabilidade-prod",

    [Parameter(Mandatory = $false)]
    [string]$PostgresPassword = ""
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$PackagePath = Join-Path $env:TEMP "chatbot-contabilidade-prod.tar.gz"
$RemoteDir = "/opt/chatbot-contabilidade"
$RemotePackage = "/tmp/chatbot-contabilidade-prod.tar.gz"
$SshTarget = "$User@$HostName"
$SshExe = (Get-Command ssh -ErrorAction SilentlyContinue).Source
$ScpExe = (Get-Command scp -ErrorAction SilentlyContinue).Source

if ([string]::IsNullOrWhiteSpace($SshExe)) {
    $SshExe = "C:\Windows\System32\OpenSSH\ssh.exe"
}

if ([string]::IsNullOrWhiteSpace($ScpExe)) {
    $ScpExe = "C:\Windows\System32\OpenSSH\scp.exe"
}

if (-not (Test-Path $SshExe)) {
    throw "ssh.exe nao encontrado. Instale o OpenSSH Client no Windows."
}

if (-not (Test-Path $ScpExe)) {
    throw "scp.exe nao encontrado. Instale o OpenSSH Client no Windows."
}

if ([string]::IsNullOrWhiteSpace($PostgresPassword)) {
    $PostgresPassword = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 32 | ForEach-Object {[char]$_})
}

$SshArgs = @()
if (-not [string]::IsNullOrWhiteSpace($SshKeyPath)) {
    $SshArgs += "-i"
    $SshArgs += $SshKeyPath
}

Write-Host "Gerando pacote do projeto..."
if (Test-Path $PackagePath) {
    Remove-Item -LiteralPath $PackagePath -Force
}

Push-Location $ProjectRoot
try {
    tar `
        --exclude=".git" `
        --exclude="target" `
        --exclude=".env" `
        --exclude=".env.production" `
        --exclude="*.log" `
        --exclude="backups" `
        --exclude="generated" `
        -czf $PackagePath .
}
finally {
    Pop-Location
}

Write-Host "Enviando pacote para $SshTarget..."
& $ScpExe @SshArgs $PackagePath "${SshTarget}:$RemotePackage"

Write-Host "Preparando servidor Oracle..."
$RemoteSetup = @"
set -euo pipefail

if command -v apt-get >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install -y docker.io docker-compose-plugin nginx
elif command -v dnf >/dev/null 2>&1; then
  sudo dnf install -y docker-cli docker-compose-plugin nginx
else
  echo "Gerenciador de pacotes nao suportado. Use Ubuntu ou Oracle Linux com dnf."
  exit 1
fi

sudo systemctl enable --now docker
sudo systemctl enable --now nginx
sudo usermod -aG docker `$(whoami) || true

sudo mkdir -p $RemoteDir
sudo chown -R `$(whoami):`$(whoami) $RemoteDir
tar -xzf $RemotePackage -C $RemoteDir
cd $RemoteDir

cat > .env.production <<'ENVEOF'
POSTGRES_DB=chatbot_contabilidade
POSTGRES_USER=chatbot
POSTGRES_PASSWORD=$PostgresPassword

WHATSAPP_CLOUD_ENABLED=true
WHATSAPP_CLOUD_API_VERSION=v23.0
WHATSAPP_CLOUD_PHONE_NUMBER_ID=$PhoneNumberId
WHATSAPP_CLOUD_ACCESS_TOKEN=$AccessToken
WHATSAPP_CLOUD_VERIFY_TOKEN=$VerifyToken
ENVEOF

docker compose -f docker-compose.prod.yml --env-file .env.production build app
docker compose -f docker-compose.prod.yml --env-file .env.production up -d

sudo cp deploy/nginx/chatbot-contabilidade.conf /etc/nginx/conf.d/chatbot-contabilidade.conf
if [ -f /etc/nginx/sites-enabled/default ]; then
  sudo rm -f /etc/nginx/sites-enabled/default
fi
sudo nginx -t
sudo systemctl reload nginx

curl -fsS "http://127.0.0.1:8080/api/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=$VerifyToken&hub.challenge=123456"
echo
docker compose -f docker-compose.prod.yml --env-file .env.production ps
"@

& $SshExe @SshArgs $SshTarget $RemoteSetup

Write-Host ""
Write-Host "Deploy concluido."
Write-Host "Webhook para a Meta:"
Write-Host "http://$HostName/api/whatsapp/webhook"
Write-Host ""
Write-Host "Verify token:"
Write-Host $VerifyToken
Write-Host ""
Write-Host "Teste no navegador:"
Write-Host "http://$HostName/api/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=$VerifyToken&hub.challenge=123456"
