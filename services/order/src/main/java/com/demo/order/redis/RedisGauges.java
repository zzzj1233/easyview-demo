package com.demo.order.redis;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisGauges {
    private final RedisOps ops;
    private final MeterRegistry registry;

    @Value("${app.redis.gauges.queues:order-delay,coupon-delay}")
    private List<String> queues;

    @Value("${app.redis.gauges.boards:hot-sku,top-buyer}")
    private List<String> boards;

    public RedisGauges(RedisOps ops, MeterRegistry registry) {
        this.ops = ops;
        this.registry = registry;
    }

    @PostConstruct
    public void register() {
        for (String q : queues) {
            Gauge.builder("redis_delayqueue_pending_size", ops, o -> (double) o.delayQueueSize(q))
                    .tag("queue", q).register(registry);
        }
        for (String b : boards) {
            Gauge.builder("redis_leaderboard_size", ops, o -> (double) o.leaderboardSize(b))
                    .tag("board", b).register(registry);
        }
    }
}
