# 04 — DB 慢查询

## 注入

```bash
curl -X POST localhost:28082/chaos/db-slow?ms=1500
```

期望 1~2 分钟触发 `DbRtHigh @ inventory`。

## SOP（计时 5 min）

### T+0:30 — 告警跳 trace

`[ALERT] DbRtHigh application=inventory`

点 trace_link 进 SkyWalking → Service `inventory` → 慢 trace。

### T+1:30 — 看 span 拆分

```
inventory: GET /api/inventory/SKU-1 ─────── 1.5s
  └─ inventory: InventoryService.getStock ── 1.5s
       └─ mysql: SELECT FROM inventory ───── 1.5s   ← DB span 占 100%
```

**判断**：DB 慢，不是 ORM / 网络 / 业务代码。

### T+2:30 — 验证 DB 端

```bash
docker compose exec mysql mysql -uroot -proot demo -e "SHOW FULL PROCESSLIST"
docker compose exec mysql mysql -uroot -proot demo -e "EXPLAIN SELECT * FROM inventory WHERE sku='SKU-1'"
```

期望看到 process 有正在执行的 SELECT，或 EXPLAIN 显示 `type=ALL`（全表扫）。

**注**：本演练里 chaos 是用 `Thread.sleep` 模拟"慢"，DB 端其实秒返回。真实生产里会看到：
- `SHOW PROCESSLIST`：有 1.5s+ 的 query
- slow log：`/var/log/mysql/slow.log`
- 慢原因：缺索引 / 全表扫 / 锁等待 / 大表 OR

### T+3:30 — 看变更

```bash
git log --oneline --since="1 hour ago"
# 真实生产看：
#   1) 是否新发布带新 SQL
#   2) 是否近期 DDL（删索引 / 改字段类型）
#   3) 是否数据量爆炸（小表突然变大表）
```

### T+4:30 — 给结论

> **故障**：inventory 调 DB 查询稳定慢 1.5s
> **故障域**：DB 端（演练用 sleep 模拟，真实情况看 slow log）
> **触发因子**：常见根因 = 索引缺失 / 大表全扫 / 锁等待
> **止血**：① 紧急加索引（若是缺索引）② kill 慢 query ③ 业务侧加缓存 / 降级

## 止血

```bash
curl -X POST localhost:28082/chaos/reset
```

## 验收

- [x] 通过 trace span 拆分判定 DB 慢，不是业务慢
- [x] 知道 `SHOW PROCESSLIST` / `EXPLAIN` / slow log 三件套
- [x] 区分"演练 sleep mock"与"真实 DB 慢"的判定差异
