package com.demo.order.redis;

import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/redis-demo")
public class RedisDemoController {
    private final RedisOps ops;

    public RedisDemoController(RedisOps ops) { this.ops = ops; }

    @PostMapping("/lock")
    public Map<String, Object> lockDemo(@RequestParam(defaultValue = "order-pay") String name,
                                        @RequestParam(defaultValue = "200") long holdMs,
                                        @RequestParam(defaultValue = "500") long waitMs) {
        String token = UUID.randomUUID().toString();
        boolean got = ops.tryLock(name, token, Duration.ofSeconds(3), Duration.ofMillis(waitMs));
        if (!got) return Map.of("acquired", false);
        try { Thread.sleep(holdMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        ops.unlock(name, token);
        return Map.of("acquired", true, "holdMs", holdMs);
    }

    @PostMapping("/ratelimit")
    public Map<String, Object> ratelimit(@RequestParam(defaultValue = "create-order") String resource,
                                         @RequestParam(defaultValue = "10") int qps) {
        boolean pass = ops.rateLimit(resource, qps);
        return Map.of("resource", resource, "qps", qps, "pass", pass);
    }

    @PostMapping("/delayq/enqueue")
    public Map<String, Object> enqueue(@RequestParam(defaultValue = "order-delay") String queue,
                                       @RequestParam(defaultValue = "30000") long delayMs) {
        String payload = "msg-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000);
        ops.enqueueDelay(queue, payload, Duration.ofMillis(delayMs));
        return Map.of("queue", queue, "payload", payload, "size", ops.delayQueueSize(queue));
    }

    @PostMapping("/delayq/poll")
    public Map<String, Object> poll(@RequestParam(defaultValue = "order-delay") String queue,
                                    @RequestParam(defaultValue = "10") int max,
                                    @RequestParam(defaultValue = "false") boolean simulateFail) {
        List<String> due = ops.pollDelayDue(queue, max);
        if (simulateFail) ops.delayQueueFailed(queue);
        return Map.of("queue", queue, "polled", due.size(), "items", due, "remain", ops.delayQueueSize(queue));
    }

    @PostMapping("/leaderboard/incr")
    public Map<String, Object> lb(@RequestParam(defaultValue = "hot-sku") String board,
                                  @RequestParam String member,
                                  @RequestParam(defaultValue = "1") double score) {
        ops.leaderboardAdd(board, member, score);
        return Map.of("board", board, "member", member, "size", ops.leaderboardSize(board));
    }

    @GetMapping("/op")
    public Map<String, Object> rawOp(@RequestParam(defaultValue = "get") String op) {
        return ops.timed(op, () -> Map.of("op", op, "ts", System.currentTimeMillis()));
    }
}
