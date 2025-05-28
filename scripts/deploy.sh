#!/bin/bash

# 작업 디렉토리를 /home/ubuntu/app으로 변경
cd /home/ubuntu/app

# 환경변수 DOCKER_APP_NAME을 spring으로 설정
DOCKER_APP_NAME=spring
# 네트워크 존재 여부 확인 및 생성
NETWORK_NAME="app-network"
# redis container명
REDIS_CONTAINER_NAME="redis"

if ! sudo docker network ls | grep -q "$NETWORK_NAME"; then
    echo "Docker 네트워크가 없습니다. $NETWORK_NAME를 생성합니다."
    sudo docker network create $NETWORK_NAME
fi

# Redis 컨테이너가 실행 중인지 확인
IS_REDIS_RUNNING=$(sudo docker ps --filter "name=${REDIS_CONTAINER_NAME}" --filter "status=running" -q)

# Redis 컨테이너가 없거나 꺼져 있으면 redis-compose로 실행
if [ -z "$IS_REDIS_RUNNING" ]; then
    echo "Redis 실행 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)" >> /home/ubuntu/deploy.log
    sudo docker-compose -f docker-compose.redis.yml up -d

    # 재확인
    sleep 3
    IS_REDIS_RUNNING=$(sudo docker ps --filter "name=${REDIS_CONTAINER_NAME}" --filter "status=running" -q)
    if [ -z "$IS_REDIS_RUNNING" ]; then
        echo "배포 중단 - Redis 컨테이너 실행 실패 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)" >> /home/ubuntu/deploy.log
        exit 1
    fi
fi

echo "Redis 상태: $(sudo docker ps -a --filter name=redis --format '{{.Status}}')" >> /home/ubuntu/deploy.log

# 실행중인 blue가 있는지 확인
# 프로젝트의 실행 중인 컨테이너를 확인하고, 해당 컨테이너가 실행 중인지 여부를 EXIST_BLUE 변수에 저장
EXIST_BLUE=$(sudo docker-compose -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml ps | grep Up)

# 배포 시작한 날짜와 시간을 기록
echo "배포 시작일자 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)" >> /home/ubuntu/deploy.log

# green이 실행중이면 blue up
# EXIST_BLUE 변수가 비어있는지 확인
if [ -z "$EXIST_BLUE" ]; then

  # 로그 파일(/home/ubuntu/deploy.log)에 "blue up - blue 배포 : port:8081"이라는 내용을 추가
  echo "blue 배포 시작 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)" >> /home/ubuntu/deploy.log

	# docker-compose.blue.yml 파일을 사용하여 spring-blue 프로젝트의 컨테이너를 빌드하고 실행
	sudo docker-compose -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml up -d --build

  # 30초 동안 대기
  sleep 30

  # /home/ubuntu/deploy.log: 로그 파일에 "green 중단 시작"이라는 내용을 추가
  echo "green 중단 시작 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)" >> /home/ubuntu/deploy.log

  # docker-compose.green.yml 파일을 사용하여 spring-green 프로젝트의 컨테이너를 중지
  ## sudo docker-compose -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml down
  # graceful shutdown을 위해 stop을 사용
  sudo docker-compose -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml stop
  sudo docker-compose -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml rm -f

   # 사용하지 않는 이미지 삭제
  sudo docker image prune -af

  echo "green 중단 완료 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)" >> /home/ubuntu/deploy.log

# blue가 실행중이면 green up
else
	echo "green 배포 시작 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)" >> /home/ubuntu/deploy.log
	sudo docker-compose -p ${DOCKER_APP_NAME}-green -f docker-compose.green.yml up -d --build

  sleep 30

  echo "blue 중단 시작 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)" >> /home/ubuntu/deploy.log
  # sudo docker-compose -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml down
  sudo docker-compose -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml stop
  sudo docker-compose -p ${DOCKER_APP_NAME}-blue -f docker-compose.blue.yml rm -f

  sudo docker image prune -af

  echo "blue 중단 완료 : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)" >> /home/ubuntu/deploy.log

fi
  echo "배포 종료  : $(date +%Y)-$(date +%m)-$(date +%d) $(date +%H):$(date +%M):$(date +%S)" >> /home/ubuntu/deploy.log

  echo "===================== 배포 완료 =====================" >> /home/ubuntu/deploy.log
  echo >> /home/ubuntu/deploy.log
