# 01 — order P99 > 1s

## 注入

```bash
curl -X POST localhost:28081/chaos/latency?ms=2000
```

期望：2 分钟内 Prometheus 触发 `ServiceRTHigh`，gateway 日志出现：

```
[ALERT] ServiceRTHigh status=firing
        application=order severity=warning
        summary    : order P99 > 1s @ host.docker.internal:8081
        trace_link : http://localhost:28180/trace?serviceName=order
        runbook    : .../runbook/01-rt-high.md
```

## SOP（计时 5 min）

### T+0:30 — 点 trace 链接

在 SkyWalking UI（:18080） → **Trace** → Service 选 `order` → 找一条最慢 trace（Duration 倒序）。

期望看到：

```
gateway: GET /api/order/1 ─────────────────────── 2.1s
  └─ order: GET /api/order/1 ─────────────────── 2.0s    ← 全在 order
       ├─ order: OrderService.getOrder ────── 2.0s        ← 函数本身 2s
       │   ├─ mysql: SELECT FROM orders ──── 5ms
       │   └─ inventory: GET /api/inventory  ── 8ms       ← 下游正常
       └─ redis: GET order:1 ──────────────── 1ms
```

**判断**：order 自身代码慢，**不是** DB / Redis / inventory。

### T+1:30 — 点 "View Logs"

在 trace 详情页右上角 → **View Logs** → 看到此 traceId 关联的日志，定位到 OrderService.getOrder 之前/之后没有任何异常 → 排除业务异常。

### T+2:30 — 看 JVM 面板

打开 Grafana → Service Overview → `app=order`

- CPU 正常（<30%）
- Heap 正常（<60%）
- GC 正常

**排除资源问题**。

### T+3:30 — 看最近变更

```bash
# mock：git log
git log --oneline --since="1 hour ago"
# 真实生产：CD 系统 / 配置中心审计日志
```

若 30 分钟内有发布 → **优先回滚**，再 debug。

本演练里你刚 curl 了 `/chaos/latency`，所以"变更" = chaos 注入。

### T+4:30 — 给结论

> **故障**：order 自身代码 sleep 注入 2s
> **故障域**：order service
> **触发因子**：chaos 注入（生产中通常是新版本引入死循环 / 错误 sleep / 锁等待）
> **影响**：所有走 `/api/order/{id}` 的请求 P99 > 2s
> **止血**：回滚到上一版本 / 关闭 chaos

## 止血

```bash
curl -X POST localhost:28081/chaos/reset
```

## 验收

T+5min 内：
- [x] 定位到 order 自身慢
- [x] 排除 DB / Redis / 下游 / GC / CPU
- [x] 找到触发因子（变更 / chaos）
- [x] 给出回滚决策
