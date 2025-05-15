#!/bin/bash

echo "[INFO] AfterInstall: d-* 캐시 정리 시작"

# 현재 실행 중인 배포 디렉토리 경로 얻기
CURRENT_DEPLOY_DIR=$(dirname "$(readlink -f "$0")")   # cleanup-after-install.sh 경로 기반
CURRENT_D_ID=$(echo "$CURRENT_DEPLOY_DIR" | grep -o 'd-[^/]*')

# 삭제할 목록에서 현재 d-ID는 제외
for d in /opt/codedeploy-agent/deployment-root/*/d-*; do
  if [[ "$d" != *"$CURRENT_D_ID"* ]]; then
    echo "[INFO] 삭제 대상: $d"
    rm -rf "$d"
  else
    echo "[INFO] 현재 배포 디렉토리 $d 는 건너뜀"
  fi
done

echo "[INFO] AfterInstall: 정리 완료"
exit 0
