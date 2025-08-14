#!/bin/bash
set -euo pipefail

APP_DIR="/home/ubuntu/app"
cd "$APP_DIR"

DOCKER_COMPOSE="sudo docker-compose"   # 네 환경 유지
DOCKER="sudo docker"
DOCKER_APP_NAME="spring"
NETWORK_NAME="app-network"
REDIS_CONTAINER_NAME="redis"

# .env 필수
if [[ ! -f ".env" ]]; then
  echo "[ERROR] .env not found in ${APP_DIR}"
  exit 1
fi

# .env 로드 (REDIS_PASSWORD 등)
set -o allexport
source .env
set +o allexport

# 네트워크 없으면 생성
if ! $DOCKER network ls | grep -q "$NETWORK_NAME"; then
  echo "Docker 네트워크가 없습니다. $NETWORK_NAME를 생성합니다."
  $DOCKER network create $NETWORK_NAME
fi

# Redis 상태 확인 후 없으면 기동 (포트 미노출, requirepass/protected-mode)
IS_REDIS_RUNNING=$($DOCKER ps --filter "name=${REDIS_CONTAINER_NAME}" --filter "status=running" -q)
if [ -z "$IS_REDIS_RUNNING" ]; then
  echo "Redis 실행 : $(date '+%F %T')" >> /home/ubuntu/deploy.log
  $DOCKER_COMPOSE --env-file .env -f docker-compose.redis.yml up -d

  # 간단 헬스체크
  sleep 3
  if ! $DOCKER exec -i ${REDIS_CONTAINER_NAME} redis-cli -a "${REDIS_PASSWORD}" PING | grep -q PONG; then
    echo "배포 중단 - Redis AUTH PING 실패 : $(date '+%F %T')" >> /home/ubuntu/deploy.log
    exit 1
  fi
fi
echo "Redis 상태: $($DOCKER ps -a --filter name=redis --format '{{.Status}}')" >> /home/ubuntu/deploy.log

# 현재 BLUE 실행 여부
EXIST_BLUE=$($DOCKER_COMPOSE -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml ps | grep Up)

echo "배포 시작일자 : $(date '+%F %T')" >> /home/ubuntu/deploy.log

if [ -z "$EXIST_BLUE" ]; then
  echo "blue 배포 시작 : $(date '+%F %T')" >> /home/ubuntu/deploy.log
  $DOCKER_COMPOSE --env-file .env -p ${DOCKER_APP_NAME}-blue  -f docker-compose.blue.yml  up -d --build

  sleep 10

  echo "green 중단 시작 : $(date '+%F %T')" >> /home/ubuntu/deploy.log
  $DOCKER_COMPOSE -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml stop || true
  $DOCKER_COMPOSE -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml rm -f || true

  $DOCKER image prune -af || true
  echo "green 중단 완료 : $(date '+%F %T')" >> /home/ubuntu/deploy.log
else
  echo "green 배포 시작 : $(date '+%F %T')" >> /home/ubuntu/deploy.log
  $DOCKER_COMPOSE --env-file .env -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml up -d --build

  sleep 10

  echo "blue 중단 시작 : $(date '+%F %T')" >> /home/ubuntu/deploy.log
  $DOCKER_COMPOSE -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml stop || true
  $DOCKER_COMPOSE -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml rm -f || true

  $DOCKER image prune -af || true
  echo "blue 중단 완료 : $(date '+%F %T')" >> /home/ubuntu/deploy.log
fi

echo "배포 종료  : $(date '+%F %T')" >> /home/ubuntu/deploy.log
echo "===================== 배포 완료 =====================" >> /home/ubuntu/deploy.log
echo >> /home/ubuntu/deploy.log
