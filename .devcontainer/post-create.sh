#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "===> [1/5] download SkyWalking Java agent 9.6.0"
if [ ! -f infra/skywalking-agent/skywalking-agent.jar ]; then
  rm -rf /tmp/skywalking-agent
  curl -sL https://dlcdn.apache.org/skywalking/java-agent/9.6.0/apache-skywalking-java-agent-9.6.0.tgz -o /tmp/sw.tgz
  tar xzf /tmp/sw.tgz -C /tmp
  cp -r /tmp/skywalking-agent/* infra/skywalking-agent/
  rm -f /tmp/sw.tgz
fi
echo "    agent at infra/skywalking-agent/skywalking-agent.jar"

echo "===> [2/5] download Arthas boot"
mkdir -p tools
if [ ! -f tools/arthas-boot.jar ]; then
  curl -sL https://arthas.aliyun.com/arthas-boot.jar -o tools/arthas-boot.jar
fi
echo "    arthas at tools/arthas-boot.jar"

echo "===> [3/5] mvn build (parallel)"
mvn -T 4 -DskipTests clean package -q

echo "===> [4/5] docker pull (preheat images)"
docker compose pull 2>&1 | tail -5 || true

echo "===> [5/5] make scripts executable"
chmod +x dev.sh services/start-all.sh services/stop-all.sh 2>/dev/null || true

echo
echo "==============================================="
echo "  post-create done. Next time codespace starts,"
echo "  post-start.sh will bring everything up."
echo "==============================================="
