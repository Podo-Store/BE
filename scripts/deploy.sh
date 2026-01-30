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
  $DC --project-name ${APP_NAME}-blue -f docker-compose.blue.yml up -d --build
  sleep 20
  $DC --project-name ${APP_NAME}-green -f docker-compose.green.yml down || true
else
  $DC --project-name ${APP_NAME}-green -f docker-compose.green.yml up -d --build
  sleep 20
  $DC --project-name ${APP_NAME}-blue -f docker-compose.blue.yml down || true
fi

$DOCKER image prune -f --filter "until=168h" || true
echo "=== ApplicationStart done $(date '+%F %T') ==="