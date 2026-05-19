#!/usr/bin/env bash
# 启动 3 个 Java 服务（顺序：inventory → order → gateway）
set -e
cd "$(dirname "$0")/.."
for app in inventory order gateway; do
  bash dev.sh $app start
done
echo
echo "all services started:"
for app in gateway order inventory; do
  bash dev.sh $app status
done
