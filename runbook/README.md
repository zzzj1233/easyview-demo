# Runbook 索引

每个 runbook 是一个 5 分钟定位的演练剧本。流程：

1. **注入**：复制命令到终端跑
2. **等告警**（≤2min）：在 gateway 日志（`./dev.sh gateway logs`）能看到 `[ALERT]` 输出，含 3 个一键链接
3. **跟着 SOP 排查**（≤3min）：从告警 → trace → log → 变更
4. **得出结论**：故障域 + 触发因子 + 止血方案
5. **reset** 恢复

| # | 故障 | 入口 | 期望 MTTI |
|---|---|---|---|
| [01](./01-rt-high.md) | order RT > 1s | `/chaos/latency` | 2~3 min |
| [02](./02-error-rate.md) | inventory 5xx 30% | `/chaos/error` | 2~3 min |
| [03](./03-jvm-fullgc.md) | order Full GC | `/chaos/oom` | 3~5 min |
| [04](./04-db-slow.md) | DB 慢查询 | `/chaos/db-slow` | 2~3 min |
| [05](./05-single-instance.md) | 单实例 CPU 100% | `/chaos/cpu` | 2 min |

## 5 分钟定位心法

> 1. **一键** 告警 → SkyWalking Trace
> 2. **一键** Trace → Log（View Logs）
> 3. **一键** Trace → 最近变更（这里用 git log mock）

三个一键全通，5 分钟定位 = 工程问题，不是经验问题。

## 通用决策树

```
告警触发
  ↓
看 SkyWalking topology → 是上游传染还是自身/下游？
  ├─ 下游（inventory 红）→ 切 02/04
  └─ 自身（order 红）
       ↓
看 JVM 面板（Grafana）
  ├─ CPU 100% → 切 05
  ├─ Full GC  → 切 03
  └─ 都正常
       ↓
看 DB / Redis RT span
  ├─ DB 慢 → 切 04
  └─ 都正常
       ↓
看最近变更
  ├─ 刚发版/配置推送 → 优先回滚
  └─ 无 → 看流量是否突增（k6-burst）
```
