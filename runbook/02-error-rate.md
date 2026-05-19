# 02 — inventory 5xx > 5%

## 注入

```bash
curl -X POST localhost:8082/chaos/error?percent=30
```

期望 1~2 分钟触发 `ServiceErrorRateHigh @ inventory`。

## SOP（计时 5 min）

### T+0:30 — SkyWalking Topology

打开 SkyWalking UI → **Topology** → 看到节点关系：

```
gateway ─→ order ─→ inventory  ←──  inventory 节点变红
```

**判断**：故障域在 inventory，order/gateway 是被动传染（也会有 5xx 因为它们调 inventory）。

### T+1:30 — 看失败 trace

Service `inventory` → Trace → 筛选 `is-error=true` → 任选一条。

```
gateway → order → inventory: GET /api/inventory/SKU-3  ─── 500
  └─ exception: chaos: injected error (errorPercent=30)
```

### T+2:30 — View Logs

trace 详情 → **View Logs** → 看到完整异常栈：

```
[ERROR] InventoryController - ...
java.lang.RuntimeException: chaos: injected error (errorPercent=30)
  at com.demo.inventory.chaos.ChaosState.maybeError(ChaosState.java:32)
  at com.demo.inventory.service.InventoryService.getStock(InventoryService.java:21)
```

**判断**：业务异常，**不是** 网络 / DB 错误。

### T+3:30 — 看变更

```bash
git log --oneline --since="1 hour ago"
```

或者：`/actuator/info`，查 buildTime / git commit。

### T+4:30 — 给结论

> **故障**：inventory.getStock 30% 概率抛 RuntimeException
> **故障域**：inventory service（不是网络 / DB）
> **触发因子**：chaos 注入（生产中常见：新版引入 NPE / 配置错误 / 下游降级未做兜底）
> **影响**：约 30% 用户请求失败，order 服务被传染 5xx 也升高
> **止血**：回滚 inventory；或临时给 order 的 InventoryClient 加 fallback 返回默认库存 0

## 止血

```bash
curl -X POST localhost:8082/chaos/reset
```

## 验收

- [x] 通过 Topology 准确定位到 inventory（不是 order / gateway）
- [x] 通过 Log 看到具体异常类与栈
- [x] 给出止血方案（回滚 + 降级双轨）
