#!/usr/bin/env sh
set -eu

APP_SERVICE=lingxi-admin
COMPOSE_FILE=docker-compose.prod.yml

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR"

if [ ! -f .env ]; then
  echo "Missing .env."
  exit 1
fi

compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose --env-file .env -f "$COMPOSE_FILE" "$@"
  elif command -v docker-compose >/dev/null 2>&1; then
    docker-compose --env-file .env -f "$COMPOSE_FILE" "$@"
  else
    echo "Docker Compose is not installed."
    exit 1
  fi
}

get_env_value() {
  key="$1"
  grep "^${key}=" .env | tail -n 1 | cut -d '=' -f 2- | tr -d '\r'
}

compose ps "$APP_SERVICE"

HEALTH_URL=$(get_env_value HEALTH_URL || true)
if [ "$HEALTH_URL" != "" ] && command -v curl >/dev/null 2>&1; then
  curl -fsS "$HEALTH_URL"
  printf "\n"
fi

compose logs --tail "${1:-120}" "$APP_SERVICE"
