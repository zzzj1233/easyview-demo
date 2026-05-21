#!/usr/bin/env bash
# 一键彻底清理 demo（停 java + 停容器 + 删卷 + 删镜像 + 提示删代码）
set -uo pipefail
cd "$(dirname "$0")/.."

echo "===> [1/4] stop java services"
bash services/stop-all.sh 2>/dev/null || true

echo
echo "===> [2/4] docker compose down -v (remove volumes)"
docker compose down -v --remove-orphans

echo
echo "===> [3/4] remove demo-specific images (skywalking, alertmanager, prometheus, grafana)"
for img in \
    apache/skywalking-oap-server:9.7.0 \
    apache/skywalking-ui:9.7.0 \
    prom/prometheus:v2.51.0 \
    prom/alertmanager:v0.27.0 \
    grafana/grafana:10.4.2 \
    elasticsearch:7.17.0 ; do
  docker rmi "$img" 2>/dev/null && echo "    removed $img" || true
done

echo
echo "===> [4/4] cleanup local files (logs, pids, target/, sw-agent)"
rm -rf services/*/target services/*/app.log services/*/app.pid infra/skywalking-agent/*

echo
echo "==============================================="
echo "  easyview-demo torn down."
echo "  To remove the source tree completely:"
echo "    cd $(dirname "$PWD") && rm -rf $(basename "$PWD")"
echo "==============================================="
