#!/bin/bash
set -euo pipefail

LOG=/home/ubuntu/deploy.log
exec >>"$LOG" 2>&1
echo "=== ApplicationStart $(date '+%F %T') ==="

cd /home/ubuntu/app

# docker-compose / compose 둘 다 대응
if command -v docker-compose >/dev/null 2>&1; then
  DC="sudo docker-compose"
else
  DC="sudo docker compose"
fi

DOCKER="sudo docker"
DOCKER_APP_NAME=spring
NETWORK_NAME=app-network
REDIS_CONTAINER_NAME=redis

# .env 존재만 확인 (값은 읽지 않음)
if [ ! -f .env ]; then
  echo "[ERROR] .env not found"; exit 1
fi

# 네트워크 생성
if ! $DOCKER network ls --format '{{.Name}}' | grep -q "^${NETWORK_NAME}$"; then
  echo "[INFO] create network ${NETWORK_NAME}"
  $DOCKER network create ${NETWORK_NAME}
fi

# Redis 없으면 compose로 올림 (항상 --env-file 사용)
IS_REDIS_RUNNING=$($DOCKER ps --filter "name=${REDIS_CONTAINER_NAME}" --filter "status=running" -q || true)
if [ -z "$IS_REDIS_RUNNING" ]; then
  echo "[INFO] Redis up via compose"
  $DC --env-file .env -f docker-compose.redis.yml up -d
  sleep 8
  IS_REDIS_RUNNING=$($DOCKER ps --filter "name=${REDIS_CONTAINER_NAME}" --filter "status=running" -q || true)
  if [ -z "$IS_REDIS_RUNNING" ]; then
    echo "[ERROR] Redis container failed to start"; exit 1
  fi
fi

# Blue/Green (항상 --env-file)
EXIST_BLUE=$($DC -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml ps | grep Up || true)
if [ -z "$EXIST_BLUE" ]; then
  echo "[INFO] blue up"
  $DC -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml --env-file .env up -d --build
  sleep 20
  echo "[INFO] stop/remove green"
  $DC -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml stop || true
  $DC -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml rm -f || true
else
  echo "[INFO] green up"
  $DC -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml --env-file .env up -d --build
  sleep 20
  echo "[INFO] stop/remove blue"
  $DC -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml stop || true
  $DC -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml rm -f || true
fi

$DOCKER image prune -af || true
echo "=== ApplicationStart done $(date '+%F %T') ==="
