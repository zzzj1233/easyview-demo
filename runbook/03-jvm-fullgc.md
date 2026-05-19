# 03 — order Full GC 频繁 / 内存溢出

## 注入

```bash
# 模拟内存泄漏：往 static List 灌 200 MB（接近 -Xmx256m 上限）
curl -X POST localhost:8081/chaos/oom?mb=200
# 再灌一次，必然 Full GC + 大概率 OOM
curl -X POST localhost:8081/chaos/oom?mb=80
```

期望 1~2 分钟触发 `JvmFullGcFrequent`（或 OOM 直接抛错触发 `ServiceErrorRateHigh`）。

## SOP（计时 5 min）

### T+0:30 — 看告警

```
[ALERT] JvmFullGcFrequent
        application=order
        summary    : order Full GC frequent
```

### T+1:00 — 看 Grafana

Service Overview → `app=order` → 看：

- **Heap Used (MB)** 面板：直线上涨到接近上限
- **GC pause rate** 面板：Full GC 频率 > 0.05/s（每分钟 3 次以上）

**判断**：内存问题，不是 RT / 错误率本质。

### T+2:00 — 看 trace 是否有变慢

SkyWalking Service `order` → Trace 列表：每条 trace 都伴随明显 GC 暂停（几百 ms 的"卡顿"散布在 span 间隙）。

### T+2:30 — 上机 jmap / jstack

```bash
# Codespaces 里
docker compose ps    # 拿不到 java pid（java 跑在宿主）
pgrep -f order-1.0.0.jar

PID=$(pgrep -f order-1.0.0.jar)
jstat -gcutil $PID 2s 5    # 看 GC 频率与各代占用
jmap -histo:live $PID | head -30   # 大对象排行
```

期望 `[B`（byte[]）排第一，holds 200+ MB → **定位到大对象 = byte[]**。

更精细：

```bash
jmap -dump:live,format=b,file=/tmp/heap.hprof $PID
# 下载下来用 MAT / JProfiler 分析，根路径 → com.demo.order.chaos.ChaosState.heapHolder
```

### T+3:30 — 看变更

```bash
git log --oneline --since="1 hour ago"
```

### T+4:30 — 给结论

> **故障**：order 堆持续上涨，Full GC 1次/秒以上，逼近 OOM
> **根因**：`ChaosState.heapHolder` static List 持有 200+ MB byte[]
> **故障域**：order service（内存泄漏）
> **触发因子**：chaos 注入（生产中常见：缓存无 LRU / 大 List 无清理 / ThreadLocal 未 remove）
> **止血**：① 立即重启 order 服务释放内存 ② 紧急回滚

## 止血

```bash
curl -X POST localhost:8081/chaos/reset   # 清 List，但不会立刻 GC
./dev.sh order restart                    # 干净重启更稳
```

## 验收

- [x] 区分"RT 慢"与"Full GC"的告警差异
- [x] 会用 jstat / jmap -histo 看堆
- [x] 知道 dump heap 的命令
