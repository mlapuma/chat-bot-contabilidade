#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/chatbot-contabilidade}"
BACKUP_DIR="${BACKUP_DIR:-$APP_DIR/backups}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

cd "$APP_DIR"
mkdir -p "$BACKUP_DIR"

if [ ! -f ".env.production" ]; then
  echo "Arquivo .env.production nao encontrado em $APP_DIR"
  exit 1
fi

set -a
source .env.production
set +a

DB_NAME="${POSTGRES_DB:-chatbot_contabilidade}"
DB_USER="${POSTGRES_USER:-chatbot}"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUTPUT="$BACKUP_DIR/${DB_NAME}-${STAMP}.sql.gz"

docker compose -f "$COMPOSE_FILE" --env-file .env.production exec -T postgres \
  pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$OUTPUT"

find "$BACKUP_DIR" -type f -name "*.sql.gz" -mtime +14 -delete

echo "Backup gerado: $OUTPUT"
