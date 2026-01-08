#!/bin/bash
set -eo pipefail   # ❗ -u 제거

LOG=/home/ubuntu/deploy.log
exec >>"$LOG" 2>&1
echo "=== ApplicationStart $(date '+%F %T') ==="

cd /home/ubuntu/app

# docker compose 판별
if command -v docker-compose >/dev/null 2>&1; then
  DC="sudo docker-compose"
else
  DC="sudo docker compose"
fi

DOCKER="sudo docker"
APP_NAME=spring
NETWORK_NAME=app-network
REDIS_CONTAINER_NAME=redis

# .env 확인
if [ ! -f .env ]; then
  echo "[ERROR] .env not found"
  exit 1
fi

# 네트워크 생성
if ! $DOCKER network ls --format '{{.Name}}' | grep -qx "$NETWORK_NAME"; then
  echo "[INFO] create network $NETWORK_NAME"
  $DOCKER network create $NETWORK_NAME
fi

# Redis 확인
if ! $DOCKER ps --filter "name=^/${REDIS_CONTAINER_NAME}$" --filter "status=running" -q | grep -q .; then
  echo "[INFO] start redis"
  $DC --env-file .env -f docker-compose.redis.yml up -d || true
  sleep 10
fi

# blue 컨테이너 존재 여부로 판단
BLUE_RUNNING=$($DOCKER ps --filter "name=${APP_NAME}-blue" --filter "status=running" -q)

if [ -z "$BLUE_RUNNING" ]; then
  echo "[INFO] deploy BLUE"
  $DC -p ${APP_NAME}-blue -f docker-compose.blue.yml --env-file .env up -d --build
  sleep 20
  echo "[INFO] stop GREEN"
  $DC -p ${APP_NAME}-green -f docker-compose.green.yml down || true
else
  echo "[INFO] deploy GREEN"
  $DC -p ${APP_NAME}-green -f docker-compose.green.yml --env-file .env up -d --build
  sleep 20
  echo "[INFO] stop BLUE"
  $DC -p ${APP_NAME}-blue -f docker-compose.blue.yml down || true
fi

$DOCKER image prune -af || true
echo "=== ApplicationStart done $(date '+%F %T') ==="
