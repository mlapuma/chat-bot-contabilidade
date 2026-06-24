# Deploy em producao com VPS

Este guia prepara o chatbot para rodar em uma VPS com PostgreSQL no mesmo servidor, Nginx com HTTPS e WhatsApp Cloud API.

## Arquitetura

```text
Cliente WhatsApp
  -> Meta WhatsApp Cloud API
  -> http://IP_PUBLICO_DA_VPS/api/whatsapp/webhook
  -> Nginx
  -> Spring Boot em 127.0.0.1:8080
  -> PostgreSQL no Docker
```

## Servidor recomendado

Para iniciar com baixo custo:

- VPS 2 GB RAM;
- Ubuntu LTS;
- Docker e Docker Compose;
- Nginx;
- PostgreSQL no mesmo `docker-compose.prod.yml`.

## URL inicial sem subdominio

Nesta primeira etapa, sem configurar subdominio, a URL sera baseada no IP publico da VPS:

```text
http://IP_PUBLICO_DA_VPS
```

Webhook da Meta:

```text
http://IP_PUBLICO_DA_VPS/api/whatsapp/webhook
```

Observacao: para testes e homologacao isso simplifica a subida. Para uso real com webhook do WhatsApp Cloud API, a Meta pode exigir uma URL publica em HTTPS. Se a Meta nao aceitar o IP em HTTP, usar temporariamente uma URL HTTPS da propria plataforma de hospedagem/tunel ou concluir o ajuste do subdominio com Nginx + Certbot.

## Subdominio depois

Quando for fazer o ajuste de dominio, criar um registro DNS:

```text
Tipo: A
Nome: api
Valor: IP_PUBLICO_DA_VPS
```

E trocar o webhook para:

```text
https://api.acsa.com.br/api/whatsapp/webhook
```
## Arquivos de producao

- `docker-compose.prod.yml`: sobe backend e PostgreSQL.
- `.env.production.example`: modelo das variaveis reais.
- `deploy/nginx/chatbot-contabilidade.conf`: template do Nginx.
- `scripts/deploy-production.sh`: build e deploy na VPS.
- `scripts/backup-postgres.sh`: backup do PostgreSQL.
- `scripts/check-production.sh`: teste de aplicacao e webhook.

## Configurar VPS

Instalar dependencias:

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin nginx git
sudo systemctl enable --now docker
```

Criar pasta:

```bash
sudo mkdir -p /opt/chatbot-contabilidade
sudo chown -R $USER:$USER /opt/chatbot-contabilidade
```

Enviar o projeto para:

```text
/opt/chatbot-contabilidade
```

## Variaveis de producao

Na VPS:

```bash
cd /opt/chatbot-contabilidade
cp .env.production.example .env.production
nano .env.production
```

Preencher:

```text
POSTGRES_PASSWORD=senha-forte
WHATSAPP_CLOUD_ENABLED=true
WHATSAPP_CLOUD_API_VERSION=v23.0
WHATSAPP_CLOUD_PHONE_NUMBER_ID=phone-number-id-do-numero-producao
WHATSAPP_CLOUD_ACCESS_TOKEN=token-permanente-da-meta
WHATSAPP_CLOUD_VERIFY_TOKEN=token-forte-de-webhook
```

Numero inicial combinado:

```text
55 11 97259-2285
```

Importante: no backend, o valor usado para envio e o `WHATSAPP_CLOUD_PHONE_NUMBER_ID`, nao o numero digitado. Esse ID vem da Meta.

## Subir aplicacao

```bash
cd /opt/chatbot-contabilidade
bash scripts/deploy-production.sh
```

### Deploy automatico pela maquina local

Se a VPS Oracle ja permite SSH, tambem e possivel fazer o envio e a configuracao inicial a partir do Windows:

```powershell
.\scripts\deploy-oracle-vps.ps1 `
  -HostName "IP_PUBLICO_DA_ORACLE" `
  -User "opc" `
  -PhoneNumberId "PHONE_NUMBER_ID_DA_META" `
  -AccessToken "TOKEN_DA_META" `
  -VerifyToken "chatbot-contabilidade-prod"
```

Esse script empacota o projeto, envia para `/opt/chatbot-contabilidade`, cria `.env.production`, instala Docker/Nginx quando necessario, sobe o PostgreSQL e o backend, e configura o Nginx para responder pelo IP publico.

Validar localmente:

```bash
curl "http://127.0.0.1:8080/api/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=SEU_VERIFY_TOKEN&hub.challenge=123456"
```

Deve retornar:

```text
123456
```

## Configurar Nginx

Copiar o template:

```bash
sudo cp deploy/nginx/chatbot-contabilidade.conf /etc/nginx/sites-available/chatbot-contabilidade.conf
sudo ln -s /etc/nginx/sites-available/chatbot-contabilidade.conf /etc/nginx/sites-enabled/chatbot-contabilidade.conf
sudo nginx -t
sudo systemctl reload nginx
```

Na etapa inicial sem subdominio, o template ja usa:

```text
server_name _;
```

Isso permite responder pelo IP publico da VPS.

## SSL e subdominio depois

Quando o subdominio estiver pronto, instalar Certbot:

```bash
sudo apt install -y certbot python3-certbot-nginx
```

Editar `/etc/nginx/sites-available/chatbot-contabilidade.conf`:

```nginx
server_name api.acsa.com.br;
```

Depois emitir SSL:

```bash
sudo certbot --nginx -d api.acsa.com.br
```

## Configurar webhook na Meta

Na Meta, usar:

```text
Callback URL: http://IP_PUBLICO_DA_VPS/api/whatsapp/webhook
Verify token: mesmo valor de WHATSAPP_CLOUD_VERIFY_TOKEN
Campo assinado: messages
```

Depois, com subdominio/SSL:

```text
Callback URL: https://api.acsa.com.br/api/whatsapp/webhook
```

## Testar producao

```bash
bash scripts/check-production.sh http://IP_PUBLICO_DA_VPS SEU_VERIFY_TOKEN
```

Depois, enviar `ola` pelo WhatsApp para o numero conectado.

## Trocar numero depois

Para trocar o numero:

1. Adicionar/verificar o novo numero na Meta.
2. Confirmar o novo `phone_number_id`.
3. Garantir que o token permanente tem permissao para o novo numero.
4. Atualizar na VPS:

```text
WHATSAPP_CLOUD_PHONE_NUMBER_ID=novo_phone_number_id
WHATSAPP_CLOUD_ACCESS_TOKEN=token-com-permissao
```

5. Reiniciar:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.production up -d
```

O webhook pode continuar o mesmo:

```text
http://IP_PUBLICO_DA_VPS/api/whatsapp/webhook
```

Quando o subdominio for configurado, o webhook passa a ser:

```text
https://api.acsa.com.br/api/whatsapp/webhook
```

## Backup

Backup manual:

```bash
bash scripts/backup-postgres.sh
```

Cron diario sugerido:

```bash
crontab -e
```

Adicionar:

```text
0 2 * * * cd /opt/chatbot-contabilidade && bash scripts/backup-postgres.sh >> backups/backup.log 2>&1
```

## Observacoes de producao

- Nao expor PostgreSQL na internet.
- Nao commitar `.env.production`.
- Usar token permanente da Meta.
- Adicionar login ao painel antes de operar com equipe real.
- Manter backups fora da VPS quando possivel.
- Custos da Meta/WhatsApp devem ser repassados ao cliente.
