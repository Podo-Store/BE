#!/bin/bash
export DOCKER_BUILDKIT=0
export COMPOSE_DOCKER_CLI_BUILD=0
set -eo pipefail

LOG_DIR=/data/home/ubuntu
LOG=$LOG_DIR/deploy.log
mkdir -p "$LOG_DIR"
exec >>"$LOG" 2>&1

echo "=== ApplicationStart $(date '+%F %T') ==="

APP_DIR=/data/home/ubuntu/app
mkdir -p "$APP_DIR"
cd "$APP_DIR" || { echo "[ERROR] APP_DIR not found"; exit 1; }

DOCKER="/usr/bin/docker"
DC="docker compose"

$DOCKER info >/dev/null 2>&1 || {
  echo "[ERROR] Docker daemon is not running"
  exit 1
}

APP_NAME=spring
NETWORK_NAME=app-network
REDIS_CONTAINER_NAME=redis

# network
if ! $DOCKER network ls --format '{{.Name}}' | grep -qx "$NETWORK_NAME"; then
  $DOCKER network create $NETWORK_NAME
fi

# redis
if ! $DOCKER ps --filter "name=^/${REDIS_CONTAINER_NAME}$" --filter "status=running" -q | grep -q .; then
  $DC -f docker-compose.redis.yml up -d
  sleep 10
fi

BLUE_RUNNING=$($DOCKER ps --filter "name=${APP_NAME}-blue" --filter "status=running" -q)

if [ -z "$BLUE_RUNNING" ]; then
  echo "[INFO] deploy BLUE"
  TARGET_CONTAINER="${APP_NAME}-blue"

  $DC --project-name ${APP_NAME}-blue -f docker-compose.blue.yml up -d --build
  sleep 5
  $DC --project-name ${APP_NAME}-green -f docker-compose.green.yml down || true
else
  echo "[INFO] deploy GREEN"
  TARGET_CONTAINER="${APP_NAME}-green"

  $DC --project-name ${APP_NAME}-green -f docker-compose.green.yml up -d --build
  sleep 5
  $DC --project-name ${APP_NAME}-blue -f docker-compose.blue.yml down || true
fi

echo "[INFO] waiting for ${TARGET_CONTAINER} to be healthy..."

# 🔑 핵심: healthcheck 대기
until $DOCKER inspect \
  --format='{{.State.Health.Status}}' "$TARGET_CONTAINER" 2>/dev/null \
  | grep -q healthy; do
  sleep 2
done

echo "[INFO] ${TARGET_CONTAINER} is healthy"

# 🔑 핵심: nginx에 새 backend 알림 (무중단)
echo "[INFO] reloading nginx"
$DOCKER exec nginx nginx -s reload

$DOCKER image prune -f --filter "until=168h" || true
echo "=== ApplicationStart done $(date '+%F %T') ==="