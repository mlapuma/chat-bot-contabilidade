#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/chatbot-contabilidade}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

cd "$APP_DIR"

if [ ! -f ".env.production" ]; then
  echo "Arquivo .env.production nao encontrado em $APP_DIR"
  echo "Copie .env.production.example para .env.production e preencha os valores reais."
  exit 1
fi

docker compose -f "$COMPOSE_FILE" --env-file .env.production pull postgres
docker compose -f "$COMPOSE_FILE" --env-file .env.production build app
docker compose -f "$COMPOSE_FILE" --env-file .env.production up -d
docker compose -f "$COMPOSE_FILE" --env-file .env.production ps

echo ""
echo "Deploy concluido."
echo "Teste local na VPS:"
echo "curl http://127.0.0.1:8080/api/whatsapp/webhook?hub.mode=subscribe\\&hub.verify_token=SEU_VERIFY_TOKEN\\&hub.challenge=123456"
