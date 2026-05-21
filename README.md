# easyview-demo — 5 分钟生产问题定位演练

一个开箱即用的可观测性 + 故障注入 demo，**目标**：让你按工程化 SOP 在 5 分钟内定位 90% 的常见生产问题。

## 架构

```
   k6 ──▶ gateway:28080 ──▶ order:28081 ──▶ inventory:28082
                                │                │
                                ▼                ▼
                              Redis            MySQL
                              :26379           :23306

  可观测栈（docker-compose，project=easyview-demo）：
    SkyWalking OAP+UI   :21800/:22800/:28180    Trace + Log
    Prometheus+Grafana  :29090/:23000           Metrics + Dashboard
    AlertManager        :29093                  → gateway /alert/webhook
    Elasticsearch       :29200                  SkyWalking 后端存储
```

> **端口约定**：所有 demo 端口 = 默认端口 **+ 20000**，避免与 dev 机已有 ES/MySQL/Redis/网关 冲突。

## 端口速查

| 服务 | 端口 | URL |
|---|---|---|
| Gateway API   | **28080** | http://172.16.50.112:28080 |
| Order         | 28081 | http://172.16.50.112:28081 |
| Inventory     | 28082 | http://172.16.50.112:28082 |
| Grafana       | **23000** | http://172.16.50.112:23000 (admin/admin) |
| Prometheus    | **29090** | http://172.16.50.112:29090 |
| AlertManager  | 29093 | http://172.16.50.112:29093 |
| SkyWalking UI | **28180** | http://172.16.50.112:28180 |
| SkyWalking gRPC | 21800 | (agent 上报) |
| MySQL (demo)  | 23306 | (内部) |
| Redis (demo)  | 26379 | (内部) |
| ES (demo)     | 29200 | (SkyWalking 用) |

debug 端口 = 服务端口 + 10000：gateway=38080 / order=38081 / inventory=38082

## 一键启动（dev 机：goldhorse / 172.16.50.112）

```bash
ssh goldhorse
mkdir -p /root/demos && cd /root/demos
git clone https://github.com/zzzj1233/easyview-demo.git
cd easyview-demo
bash scripts/setup.sh
```

setup.sh 会：
1. 下 SkyWalking Java agent 9.6.0
2. mvn build 3 个服务
3. `docker compose up -d`（project=`easyview-demo`，容器/卷自动加前缀，不污染其他 demo）
4. 启 3 个 Java 服务（带 `-javaagent` 接 SkyWalking）

## 一键彻底清理

```bash
cd /root/demos/easyview-demo
bash scripts/teardown.sh

# 连源码也删掉：
cd /root/demos && rm -rf easyview-demo
```

teardown 做了：
- 停 3 个 Java 服务
- `docker compose down -v --remove-orphans`（删容器 + 删卷）
- 删 SkyWalking / Grafana / Prometheus / ES 等镜像
- 清 target / 日志 / SW agent

## 故障演练剧本

| # | 故障 | 命令 | 期望 MTTI |
|---|---|---|---|
| [01](./runbook/01-rt-high.md) | order P99 > 1s | `curl -X POST localhost:28081/chaos/latency?ms=2000` | 2~3 min |
| [02](./runbook/02-error-rate.md) | inventory 5xx 30% | `curl -X POST localhost:28082/chaos/error?percent=30` | 2~3 min |
| [03](./runbook/03-jvm-fullgc.md) | order Full GC | `curl -X POST localhost:28081/chaos/oom?mb=200` | 3~5 min |
| [04](./runbook/04-db-slow.md) | DB 慢查询 | `curl -X POST localhost:28082/chaos/db-slow?ms=1500` | 2~3 min |
| [05](./runbook/05-single-instance.md) | CPU 100% | `curl -X POST localhost:28082/chaos/cpu?seconds=300` | 2 min |

每个剧本配套：注入命令 + 期望告警 + SOP 决策树 + 验收清单。

## 开发循环

修改 Java 代码后：

```bash
./dev.sh order restart      # build + 重启（约 25s）
./dev.sh order logs         # tail 实时日志
./dev.sh order status       # 状态查询
./dev.sh order stop
```

devtools 已加（保存 .class 自动 2~5s 热重启）。

## 在本机访问 dev 上的 UI（端口转发）

```bash
# 把 dev 上 4 个面板转到本机
ssh -fN \
  -L 23000:localhost:23000 \
  -L 28180:localhost:28180 \
  -L 29090:localhost:29090 \
  -L 28080:localhost:28080 \
  goldhorse

# 然后浏览器开：
#   Grafana       http://localhost:23000
#   SkyWalking UI http://localhost:28180
#   Prometheus    http://localhost:29090
#   Gateway API   http://localhost:28080
```

撤端口转发：`pkill -f "ssh -fN.*goldhorse"`

## 工程化要点（面试用）

| 要点 | 落地 |
|---|---|
| 告警自带 traceId / dashboard / runbook 3 个一键链接 | `infra/prometheus/alert-rules.yml` annotations |
| trace / log / metric 三大可观测打通 | logback `[%X{traceId:-N/A}][%tid]` + `GRPCLogClientAppender` |
| 一服务一面板（RED + JVM + DB） | `infra/grafana/dashboards/service-overview.json` |
| 跨服务 traceId 透传 | SkyWalking Java agent 自动织入 |
| Runbook 即文档即执行 | `runbook/*.md` 全部可复制运行 |
| 端口偏移避免冲突 | demo 端口 = 标准端口 + 20000 |
| 项目级 docker namespace | `name: easyview-demo` |

## 目录结构

```
easyview-demo/
├── docker-compose.yml    可观测栈 + MySQL/Redis（project=easyview-demo）
├── pom.xml               父 pom 聚合 3 module
├── services/
│   ├── gateway/          网关 + 告警 webhook 接收
│   ├── order/            订单（MySQL + Redis cache + Feign 调下游）
│   └── inventory/        库存（MySQL）
├── infra/
│   ├── prometheus/       抓取 + 5 条告警规则
│   ├── alertmanager/     路由到 gateway webhook
│   ├── grafana/          数据源 + dashboard JSON
│   ├── skywalking-agent/ setup 自动下载到此（已 .gitignore）
│   └── mysql-init/       schema + 种子数据
├── load/                 k6 压测脚本 + .http 请求集
├── runbook/              5 个演练剧本
├── scripts/
│   ├── setup.sh          一键启动
│   └── teardown.sh       一键彻底清理
└── dev.sh                单服务启动/重启/查日志脚本
```

## 推荐的"统一 demo 目录"

```
/root/demos/                ← 所有 demo 的根
├── easyview-demo/          ← 本 demo
└── (其它 demo)/

# 全清空：
docker ps -a --filter "name=easyview-demo" -q | xargs -r docker rm -f
rm -rf /root/demos
```

## License

MIT
