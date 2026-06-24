param(
    [string]$PhoneNumberId = $env:WHATSAPP_CLOUD_PHONE_NUMBER_ID,
    [string]$AccessToken = $env:WHATSAPP_CLOUD_ACCESS_TOKEN,
    [string]$VerifyToken = "chatbot-contabilidade-local"
)

$ErrorActionPreference = "Stop"

if (-not $PhoneNumberId -or $PhoneNumberId -eq "COLE_AQUI_O_PHONE_NUMBER_ID") {
    throw "Informe WHATSAPP_CLOUD_PHONE_NUMBER_ID ou passe -PhoneNumberId."
}

if (-not $AccessToken -or $AccessToken -eq "COLE_AQUI_O_ACCESS_TOKEN") {
    throw "Informe WHATSAPP_CLOUD_ACCESS_TOKEN ou passe -AccessToken."
}

$env:PATH = "C:\Windows\System32\WindowsPowerShell\v1.0;C:\Windows\System32;C:\Windows;C:\Program Files\Maven\bin;C:\Program Files\Java\jdk-17\bin;" + $env:PATH
$env:SPRING_DATASOURCE_URL = "jdbc:h2:mem:chatbot;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
$env:SPRING_DATASOURCE_USERNAME = "sa"
$env:SPRING_DATASOURCE_PASSWORD = ""
$env:SPRING_FLYWAY_ENABLED = "false"
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = "create-drop"
$env:WHATSAPP_CLOUD_ENABLED = "true"
$env:WHATSAPP_CLOUD_API_VERSION = "v23.0"
$env:WHATSAPP_CLOUD_PHONE_NUMBER_ID = $PhoneNumberId
$env:WHATSAPP_CLOUD_ACCESS_TOKEN = $AccessToken
$env:WHATSAPP_CLOUD_VERIFY_TOKEN = $VerifyToken

Write-Host "Subindo backend em http://localhost:8080"
Write-Host "Webhook local: http://localhost:8080/api/whatsapp/webhook"
Write-Host "Verify token: $VerifyToken"

mvn -gs .mvn/settings.xml -s .mvn/settings.xml -Dspring-boot.run.useTestClasspath=true spring-boot:run
