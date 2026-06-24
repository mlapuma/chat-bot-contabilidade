param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$VerifyToken = "chatbot-contabilidade-local"
)

$ErrorActionPreference = "Stop"

$url = "$BaseUrl/api/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=$VerifyToken&hub.challenge=123456"
$response = Invoke-WebRequest -Uri $url -UseBasicParsing

Write-Host "Status: $($response.StatusCode)"
Write-Host "Resposta: $($response.Content)"

if ($response.Content -ne "123456") {
    throw "Webhook nao devolveu o challenge esperado."
}
