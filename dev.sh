#!/usr/bin/env bash
# 1): build    增量编译指定服务（含依赖模块）
# 2): start    启动服务（带 SkyWalking agent + debug 端口）
# 3): stop     停止服务
# 4): restart  build + stop + start
# 5): logs     tail 实时日志
# 6): status   查看运行状态

set -euo pipefail
cd "$(dirname "$0")"

APP=${1:-}
ACTION=${2:-restart}

if [ -z "$APP" ]; then
  echo "Usage: $0 <gateway|order|inventory> [build|start|stop|restart|logs|status]"
  exit 1
fi

case "$APP" in
  gateway)   PORT=28080 ;;
  order)     PORT=28081 ;;
  inventory) PORT=28082 ;;
  *) echo "unknown app $APP"; exit 1 ;;
esac

SW_AGENT="$PWD/infra/skywalking-agent/skywalking-agent.jar"
LOG="services/$APP/app.log"
PID_FILE="services/$APP/app.pid"
JAR="services/$APP/target/$APP-1.0.0.jar"
DEBUG_PORT=$((PORT + 10000))

build() {
  echo ">>> mvn build $APP (incremental)"
  mvn -pl "services/$APP" -am -DskipTests package -q
}

stop() {
  if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if kill -0 "$PID" 2>/dev/null; then
      kill "$PID"
      for i in 1 2 3 4 5 6 7 8 9 10; do
        kill -0 "$PID" 2>/dev/null || break
        sleep 1
      done
      kill -9 "$PID" 2>/dev/null || true
    fi
    rm -f "$PID_FILE"
  fi
  pkill -f "$APP-1.0.0.jar" 2>/dev/null || true
}

start() {
  if [ ! -f "$JAR" ]; then build; fi
  if [ ! -f "$SW_AGENT" ]; then
    echo "!!! SkyWalking agent missing at $SW_AGENT"
    echo "    run: bash .devcontainer/post-create.sh"
    exit 1
  fi
  nohup java \
    -javaagent:"$SW_AGENT" \
    -Dskywalking.agent.service_name="$APP" \
    -Dskywalking.collector.backend_service=localhost:21800 \
    -Dskywalking.logging.level=WARN \
    -Dserver.port=$PORT \
    -Xmx256m -Xms128m \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT \
    -jar "$JAR" \
    > "$LOG" 2>&1 &
  echo $! > "$PID_FILE"
  echo ">>> $APP pid=$(cat $PID_FILE) port=$PORT debug=$DEBUG_PORT"
  echo ">>> waiting ready..."
  for i in $(seq 1 60); do
    grep -q "Started .*Application" "$LOG" 2>/dev/null && { echo ">>> ready in ${i}s"; return; }
    sleep 1
  done
  echo "!!! not ready in 60s, check $LOG"
  exit 1
}

status() {
  if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
    echo "$APP RUNNING pid=$(cat $PID_FILE) port=$PORT"
  else
    echo "$APP STOPPED"
  fi
}

case "$ACTION" in
  build)   build ;;
  start)   start ;;
  stop)    stop ;;
  restart) build; stop; start ;;
  logs)    tail -f "$LOG" ;;
  status)  status ;;
  *) echo "unknown action $ACTION"; exit 1 ;;
esac
