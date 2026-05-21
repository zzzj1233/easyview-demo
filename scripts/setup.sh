#!/usr/bin/env bash
# 一键拉起整个 demo（dev 机：goldhorse 172.16.50.112）
# 步骤：1) 下 SkyWalking agent  2) mvn build  3) docker compose up  4) 启 3 个 Java 服务
set -euo pipefail
cd "$(dirname "$0")/.."

ROOT=$PWD
SW_AGENT_DIR="$ROOT/infra/skywalking-agent"

echo "===> [1/4] download SkyWalking Java agent 9.6.0"
if [ ! -f "$SW_AGENT_DIR/skywalking-agent.jar" ]; then
  rm -rf /tmp/skywalking-agent /tmp/sw.tgz
  curl -fsSL https://dlcdn.apache.org/skywalking/java-agent/9.6.0/apache-skywalking-java-agent-9.6.0.tgz -o /tmp/sw.tgz
  tar xzf /tmp/sw.tgz -C /tmp
  cp -r /tmp/skywalking-agent/* "$SW_AGENT_DIR/"
  rm -rf /tmp/skywalking-agent /tmp/sw.tgz
fi
echo "    OK: $SW_AGENT_DIR/skywalking-agent.jar"

echo
echo "===> [2/4] mvn build (parallel)"
mvn -T 4 -DskipTests clean package -q

echo
echo "===> [3/4] docker compose up -d (project=easyview-demo)"
docker compose up -d
echo "    waiting for SkyWalking OAP ready (gRPC 21800)..."
for i in $(seq 1 60); do
  (echo > /dev/tcp/127.0.0.1/21800) >/dev/null 2>&1 && { echo "    OAP ready in ${i}s"; break; }
  sleep 2
done
echo "    waiting for MySQL ready..."
for i in $(seq 1 60); do
  docker compose exec -T mysql mysqladmin ping -uroot -proot >/dev/null 2>&1 && { echo "    MySQL ready in ${i}s"; break; }
  sleep 2
done

echo
echo "===> [4/4] start 3 java services"
chmod +x dev.sh services/start-all.sh services/stop-all.sh scripts/*.sh
bash services/start-all.sh

echo
HOST=$(hostname -I 2>/dev/null | awk '{print $1}')
[ -z "$HOST" ] && HOST=localhost
cat <<EOF

===============================================
  easyview-demo is up. Endpoints:

    Grafana       http://${HOST}:23000   (admin/admin)
    SkyWalking UI http://${HOST}:28180
    Prometheus    http://${HOST}:29090
    AlertManager  http://${HOST}:29093
    Gateway API   http://${HOST}:28080

  Generate traffic:    k6 run load/k6-baseline.js
  Inject scenario 01:  curl -X POST localhost:28081/chaos/latency?ms=2000
  Tear down:           bash scripts/teardown.sh
===============================================
EOF
