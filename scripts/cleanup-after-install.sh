#!/bin/bash

echo "[INFO] AfterInstall: d-* 캐시 정리 시작"

# d- 접두어로 시작하는 캐시 디렉토리만 정리
sudo rm -rf /opt/codedeploy-agent/deployment-root/*/d-*

echo "[INFO] AfterInstall: 정리 완료"
exit 0