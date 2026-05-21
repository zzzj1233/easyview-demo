package com.demo.order.redis;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RedisOps {
    private static final Logger log = LoggerFactory.getLogger(RedisOps.class);
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final StringRedisTemplate redis;
    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Long> lockHoldStart = new ConcurrentHashMap<>();

    public RedisOps(StringRedisTemplate redis, MeterRegistry registry) {
        this.redis = redis;
        this.registry = registry;
    }

    public <T> T timed(String op, Supplier<T> action) {
        Timer.Sample s = Timer.start(registry);
        String result = "ok";
        try {
            return action.get();
        } catch (RuntimeException e) {
            result = "error";
            throw e;
        } finally {
            s.stop(Timer.builder("redis_ops_seconds")
                    .tag("op", op).tag("result", result)
                    .publishPercentileHistogram()
                    .register(registry));
        }
    }

    public boolean tryLock(String name, String token, Duration ttl, Duration waitMax) {
        long start = System.nanoTime();
        long deadline = start + waitMax.toNanos();
        String key = "lock:" + name;
        while (true) {
            Boolean ok = redis.opsForValue().setIfAbsent(key, token, ttl);
            if (Boolean.TRUE.equals(ok)) {
                Timer.builder("redis_lock_wait_seconds").tag("name", name)
                        .publishPercentileHistogram().register(registry)
                        .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                Counter.builder("redis_lock_acquire_total")
                        .tag("name", name).tag("result", "success")
                        .register(registry).increment();
                lockHoldStart.put(name + ":" + token, System.nanoTime());
                return true;
            }
            if (System.nanoTime() >= deadline) {
                Counter.builder("redis_lock_acquire_total")
                        .tag("name", name).tag("result", "timeout")
                        .register(registry).increment();
                return false;
            }
            try { Thread.sleep(5); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
        }
    }

    public void unlock(String name, String token) {
        Long s = lockHoldStart.remove(name + ":" + token);
        if (s != null) {
            Timer.builder("redis_lock_hold_seconds").tag("name", name)
                    .publishPercentileHistogram().register(registry)
                    .record(System.nanoTime() - s, TimeUnit.NANOSECONDS);
        }
        redis.execute((org.springframework.data.redis.connection.RedisConnection c) ->
                c.scriptingCommands().eval(UNLOCK_LUA.getBytes(),
                        org.springframework.data.redis.connection.ReturnType.INTEGER,
                        1, ("lock:" + name).getBytes(), token.getBytes()));
    }

    public boolean rateLimit(String resource, int qps) {
        long bucket = System.currentTimeMillis() / 1000;
        String key = "rl:" + resource + ":" + bucket;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) redis.expire(key, Duration.ofSeconds(2));
        boolean pass = count != null && count <= qps;
        Counter.builder("redis_ratelimit_total")
                .tag("resource", resource).tag("result", pass ? "pass" : "reject")
                .register(registry).increment();
        return pass;
    }

    public void enqueueDelay(String queue, String payload, Duration delay) {
        timed("delayq_enqueue", () -> {
            redis.opsForZSet().add("dq:" + queue, payload, System.currentTimeMillis() + delay.toMillis());
            return null;
        });
    }

    public List<String> pollDelayDue(String queue, int max) {
        return timed("delayq_poll", () -> {
            long now = System.currentTimeMillis();
            Set<String> due = redis.opsForZSet().rangeByScore("dq:" + queue, 0, now, 0, max);
            if (due == null || due.isEmpty()) return Collections.emptyList();
            redis.opsForZSet().remove("dq:" + queue, due.toArray());
            return List.copyOf(due);
        });
    }

    public long delayQueueSize(String queue) {
        Long s = redis.opsForZSet().zCard("dq:" + queue);
        return s == null ? 0 : s;
    }

    public void delayQueueFailed(String queue) {
        Counter.builder("redis_delayqueue_failed_total")
                .tag("queue", queue).register(registry).increment();
    }

    public void leaderboardAdd(String board, String member, double score) {
        timed("zadd", () -> { redis.opsForZSet().add("lb:" + board, member, score); return null; });
    }

    public long leaderboardSize(String board) {
        Long s = redis.opsForZSet().zCard("lb:" + board);
        return s == null ? 0 : s;
    }
}
