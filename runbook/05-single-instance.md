# 05 — 单实例 CPU 100%

## 注入

```bash
curl -X POST localhost:8082/chaos/cpu?seconds=300
```

期望 1 分钟内触发 `SingleInstanceCpuHigh @ inventory`。

## SOP（计时 5 min）

### T+0:30 — 告警

```
[ALERT] SingleInstanceCpuHigh application=inventory
        summary: inventory CPU > 85%
```

### T+1:00 — Grafana 按 instance 切

Service Overview → `app=inventory` → **JVM CPU %** 面板。

在本演练（单实例）下：CPU 一条线翘到 100%。

> 在生产多实例场景下，关键是用 `by (instance)` 看是否**只有 1 条线翘**：
>
> ```
> CPU %
> 100%  ┤    ╱──────────       ← inventory-pod-3 (单点故障)
>  50%  ┤
>   0%  ┤────────────────       ← inventory-pod-1, pod-2, pod-4...
> ```
>
> **判断**：单实例问题 → 不是代码 bug（否则所有实例都会一样），多半是：
> - 节点硬件问题
> - 流量倾斜（热点 key 路由到单实例）
> - 单实例的死循环（如某线程跑飞）

### T+2:00 — 上机 jstack 找烧 CPU 的线程

```bash
PID=$(pgrep -f inventory-1.0.0.jar)

# 看哪个线程吃 CPU
top -H -p $PID
# 记下高 CPU 的 native tid（十进制），转 16 进制：
printf '%x\n' <tid>

# jstack 找对应栈
jstack $PID | grep -A 30 "nid=0x<16进制tid>"
```

期望看到：

```
"chaos-cpu-0" nid=0x... runnable
   at com.demo.inventory.chaos.ChaosState.lambda$burnCpu$0(ChaosState.java:42)
   ...
```

→ **定位到具体方法**：`ChaosState.burnCpu`

### T+3:00 — 看变更 + 流量

```bash
git log --oneline --since="1 hour ago"
# 是不是热点 key？
docker compose exec redis redis-cli --hotkeys
```

### T+4:00 — 给结论

> **故障**：inventory 单实例 CPU 100%（如果生产多实例，则只有 1 个 pod）
> **故障域**：单实例线程死循环（chaos-cpu-N 线程）
> **触发因子**：chaos 注入（生产中常见：死循环 / 正则回溯 / Fastjson autoType 攻击）
> **止血**：
> - 多实例：**立即摘节点**（k8s `cordon` + `drain` 或 Service 摘除），让流量打到健康实例
> - 单实例：重启服务

## 止血

```bash
# 等 300s 自然结束，或者：
./dev.sh inventory restart
```

## 验收

- [x] 通过 `by (instance)` label 切，能识别单实例 vs 全实例问题
- [x] 会用 `top -H` + `jstack` 定位烧 CPU 的具体线程与方法
- [x] 知道多实例下的止血策略（摘节点 > 重启）
