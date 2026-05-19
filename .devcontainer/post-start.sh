#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "===> bring up observability stack + MySQL + Redis"
docker compose up -d

echo "===> wait for SkyWalking OAP ready (max 90s)"
for i in $(seq 1 45); do
  if curl -sf http://localhost:12800/internal/l7check >/dev/null 2>&1 || \
     nc -z localhost 11800 2>/dev/null; then
    echo "    OAP ready"
    break
  fi
  sleep 2
done

echo "===> wait for MySQL ready (max 60s)"
for i in $(seq 1 30); do
  if docker compose exec -T mysql mysqladmin ping -uroot -proot >/dev/null 2>&1; then
    echo "    MySQL ready"
    break
  fi
  sleep 2
done

echo "===> start 3 java services (gateway/order/inventory)"
bash services/start-all.sh

echo
echo "==============================================="
echo "  Ready. Open these ports tab in VSCode:"
echo "    Grafana       :3000  (admin/admin)"
echo "    SkyWalking UI :18080"
echo "    Prometheus    :9090"
echo "    Gateway API   :8080"
echo "  Run a baseline load:"
echo "    k6 run load/k6-baseline.js"
echo "  Inject a fault (scenario 01 RT high):"
echo "    curl -X POST localhost:8081/chaos/latency?ms=2000"
echo "==============================================="
