# easyview-demo — 5 分钟生产问题定位演练

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/zzzj1233/easyview-demo?quickstart=1)

一个开箱即用的可观测性 + 故障注入 demo，**目标**：让你按工程化 SOP 在 5 分钟内定位 90% 的常见生产问题。

## 架构

```
   k6 ──▶ gateway:8080 ──▶ order:8081 ──▶ inventory:8082
                              ↓               ↓
                            Redis           MySQL
                              ↓
                            MySQL

  可观测栈（docker-compose）：
    SkyWalking OAP + UI    Trace + Log
    Prometheus + Grafana   Metrics + Dashboard
    AlertManager           告警路由（→ gateway /alert/webhook）
    Elasticsearch          SkyWalking 后端存储
```

## 快速开始（Codespaces）

1. 点上面的 **Open in GitHub Codespaces** 按钮
2. 等 ~3 分钟（首次构建：拉镜像 + mvn package + 启动）
3. VSCode 底部 **PORTS** tab 会看到：
   - `:3000` Grafana（admin/admin）
   - `:18080` SkyWalking UI
   - `:9090` Prometheus
   - `:8080` Gateway API
4. 跑基线压测：
   ```bash
   k6 run load/k6-baseline.js
   ```
5. 选一个剧本演练：[runbook/](./runbook/)

## 本地启动

```bash
docker compose up -d
./services/start-all.sh
k6 run load/k6-baseline.js
```

## 故障演练剧本

| # | 故障 | 命令 | 期望 MTTI |
|---|---|---|---|
| [01](./runbook/01-rt-high.md) | order P99 > 1s | `curl -X POST localhost:8081/chaos/latency?ms=2000` | 2~3 min |
| [02](./runbook/02-error-rate.md) | inventory 5xx 30% | `curl -X POST localhost:8082/chaos/error?percent=30` | 2~3 min |
| [03](./runbook/03-jvm-fullgc.md) | order Full GC | `curl -X POST localhost:8081/chaos/oom?mb=200` | 3~5 min |
| [04](./runbook/04-db-slow.md) | DB 慢查询 | `curl -X POST localhost:8082/chaos/db-slow?ms=1500` | 2~3 min |
| [05](./runbook/05-single-instance.md) | CPU 100% | `curl -X POST localhost:8082/chaos/cpu?seconds=300` | 2 min |

每个剧本配套：注入命令 + 期望告警 + SOP 决策树 + 验收清单。

## 开发循环

改代码 → 保存（devtools 自动 2~5s 热重启）→ 浏览器看 trace 立刻反映新逻辑。

```bash
./dev.sh order restart      # 完整重启 order（约 25s）
./dev.sh order logs         # tail 日志
./dev.sh order status       # 状态
./dev.sh order stop
```

debugger 端口约定：`5000 + service port`，即 gateway=15080 / order=15081 / inventory=15082。

## 工程化要点

| 要点 | 落地 |
|---|---|
| 告警自带 traceId / dashboard / runbook 3 个一键链接 | `infra/prometheus/alert-rules.yml` annotations |
| trace / log / metric 三大可观测打通 | logback `[%X{traceId:-N/A}][%tid]` + GRPCLogClientAppender |
| 一服务一面板（RED + JVM + DB） | `infra/grafana/dashboards/service-overview.json` |
| 跨服务 traceId 透传 | SkyWalking Java agent 自动织入 |
| Runbook 即文档即执行 | `runbook/*.md` 全部可复制运行 |

## 端口速查

| 端口 | 服务 | URL（Codespaces 会自动转 HTTPS） |
|---|---|---|
| 3000 | Grafana | https://...-3000.app.github.dev |
| 9090 | Prometheus | https://...-9090.app.github.dev |
| 9093 | AlertManager | private |
| 18080 | SkyWalking UI | https://...-18080.app.github.dev |
| 8080 | Gateway | https://...-8080.app.github.dev |
| 8081 | Order | private |
| 8082 | Inventory | private |
| 11800 | SkyWalking gRPC | internal |
| 12800 | SkyWalking REST | internal |
| 3306 | MySQL | private |
| 6379 | Redis | private |

## 目录结构

```
easyview-demo/
├── .devcontainer/        Codespaces 启动配置
├── docker-compose.yml    可观测栈 + MySQL/Redis
├── pom.xml               父 pom 聚合 3 module
├── services/
│   ├── gateway/          网关 + 告警 webhook 接收
│   ├── order/            订单（MySQL + Redis cache + Feign 调下游）
│   └── inventory/        库存（MySQL）
├── infra/
│   ├── prometheus/       抓取 + 5 条告警规则
│   ├── alertmanager/     路由到 gateway webhook
│   ├── grafana/          数据源 + dashboard JSON
│   ├── skywalking-agent/ post-create 自动下载到此
│   └── mysql-init/       schema + 种子数据
├── load/                 k6 压测脚本 + .http 请求集
├── runbook/              5 个演练剧本
└── dev.sh                统一启动/重启/查日志脚本
```

## License

MIT
