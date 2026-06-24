#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://IP_PUBLICO_DA_VPS}"
VERIFY_TOKEN="${2:-}"

if [ -z "$VERIFY_TOKEN" ]; then
  echo "Uso: ./scripts/check-production.sh http://IP_PUBLICO_DA_VPS SEU_VERIFY_TOKEN"
  echo "Depois, quando houver subdominio: ./scripts/check-production.sh https://api.acsa.com.br SEU_VERIFY_TOKEN"
  exit 1
fi

echo "Testando pagina inicial..."
curl -fsS "$BASE_URL/" >/dev/null

echo "Testando webhook..."
CHALLENGE="123456"
RESPONSE="$(curl -fsS "$BASE_URL/api/whatsapp/webhook?hub.mode=subscribe&hub.verify_token=$VERIFY_TOKEN&hub.challenge=$CHALLENGE")"

if [ "$RESPONSE" != "$CHALLENGE" ]; then
  echo "Webhook respondeu '$RESPONSE', esperado '$CHALLENGE'"
  exit 1
fi

echo "OK: aplicacao e webhook respondendo."
