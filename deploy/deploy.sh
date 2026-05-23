#!/usr/bin/env sh
set -eu

APP_SERVICE=lingxi-admin
APP_SERVICES="lingxi-admin"
COMPOSE_FILE=docker-compose.prod.yml

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR"

usage() {
  echo "Usage: ./deploy.sh [image-tag]"
  echo "Example: ./deploy.sh abc1234"
}

if [ ! -f .env ]; then
  echo "Missing .env. Copy .env.example to .env and fill production values first."
  exit 1
fi

update_env_value() {
  key="$1"
  value="$2"
  tmp_file=".env.tmp.$$"

  if grep -q "^${key}=" .env; then
    sed "s|^${key}=.*|${key}=${value}|" .env > "$tmp_file"
    mv "$tmp_file" .env
  else
    printf "\n%s=%s\n" "$key" "$value" >> .env
  fi
}

get_env_value() {
  key="$1"
  grep "^${key}=" .env | tail -n 1 | cut -d '=' -f 2- | tr -d '\r'
}

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

if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
  usage
  exit 0
fi

if [ "${1:-}" != "" ]; then
  update_env_value IMAGE_TAG "$1"
fi

IMAGE_REPOSITORY=$(get_env_value IMAGE_REPOSITORY || true)
IMAGE_TAG=$(get_env_value IMAGE_TAG || true)

if [ "$IMAGE_REPOSITORY" = "" ] || [ "$IMAGE_TAG" = "" ] || [ "$IMAGE_TAG" = "replace-with-github-sha" ]; then
  usage
  echo "IMAGE_REPOSITORY and IMAGE_TAG must be configured."
  exit 1
fi

echo "Deploying ${IMAGE_REPOSITORY}:${IMAGE_TAG}"

mkdir -p logs uploadPath
chown -R 10001:10001 logs uploadPath 2>/dev/null || chmod 777 logs uploadPath 2>/dev/null || true

compose pull $APP_SERVICES
compose up -d $APP_SERVICES
compose ps $APP_SERVICES

HEALTH_URL=$(get_env_value HEALTH_URL || true)
HEALTH_RETRY=$(get_env_value HEALTH_RETRY || true)
HEALTH_INTERVAL=$(get_env_value HEALTH_INTERVAL || true)
HEALTH_RETRY=${HEALTH_RETRY:-30}
HEALTH_INTERVAL=${HEALTH_INTERVAL:-3}

if [ "$HEALTH_URL" != "" ]; then
  if ! command -v curl >/dev/null 2>&1; then
    echo "curl is not installed. Skip health check."
    exit 0
  fi

  echo "Checking ${HEALTH_URL}"
  i=1
  while [ "$i" -le "$HEALTH_RETRY" ]; do
    if curl -fsS "$HEALTH_URL" >/dev/null; then
      echo "Health check passed."
      exit 0
    fi
    sleep "$HEALTH_INTERVAL"
    i=$((i + 1))
  done

  echo "Health check failed. Recent logs:"
  compose logs --tail 120 "$APP_SERVICE"
  exit 1
fi

echo "HEALTH_URL is empty. Skipped curl health check."
