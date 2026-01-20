#!/bin/bash
set -eo pipefail

LOG=/home/ubuntu/deploy.log
mkdir -p /home/ubuntu
exec >>"$LOG" 2>&1

echo "=== ApplicationStart $(date '+%F %T') ==="

cd /opt/codedeploy-agent/deployment-root/*/*/deployment-archive

DOCKER="docker"
DC="docker-compose"

APP_NAME=spring
NETWORK_NAME=app-network
REDIS_CONTAINER_NAME=redis

# 네트워크 생성
if ! $DOCKER network ls --format '{{.Name}}' | grep -qx "$NETWORK_NAME"; then
  echo "[INFO] create network $NETWORK_NAME"
  $DOCKER network create $NETWORK_NAME
fi

# Redis
if ! $DOCKER ps --filter "name=^/${REDIS_CONTAINER_NAME}$" --filter "status=running" -q | grep -q .; then
  echo "[INFO] start redis"
  $DC -f docker-compose.redis.yml up -d
  sleep 10
fi

BLUE_RUNNING=$($DOCKER ps --filter "name=${APP_NAME}-blue" --filter "status=running" -q)

if [ -z "$BLUE_RUNNING" ]; then
  echo "[INFO] deploy BLUE"
  $DC -p ${APP_NAME}-blue -f docker-compose.blue.yml up -d --build
  sleep 20
  $DC -p ${APP_NAME}-green -f docker-compose.green.yml down || true
else
  echo "[INFO] deploy GREEN"
  $DC -p ${APP_NAME}-green -f docker-compose.green.yml up -d --build
  sleep 20
  $DC -p ${APP_NAME}-blue -f docker-compose.blue.yml down || true
fi

$DOCKER image prune -af || true
echo "=== ApplicationStart done $(date '+%F %T') ==="
