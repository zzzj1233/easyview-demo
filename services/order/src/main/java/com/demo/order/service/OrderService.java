package com.demo.order.service;

import com.demo.order.chaos.ChaosState;
import com.demo.order.client.InventoryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final InventoryClient inventoryClient;
    private final ChaosState chaos;

    public OrderService(JdbcTemplate jdbc, StringRedisTemplate redis,
                        InventoryClient inventoryClient, ChaosState chaos) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.inventoryClient = inventoryClient;
        this.chaos = chaos;
    }

    public Map<String, Object> getOrder(Long id) {
        chaos.maybeLatency();
        chaos.maybeError();

        String cacheKey = "order:" + id;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) log.debug("cache hit id={}", id);

        chaos.maybeDbDelay();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, sku, qty, status, intent_id, created_at FROM orders WHERE id = ?", id);
        if (rows.isEmpty()) {
            log.warn("order not found id={}", id);
            return Map.of("id", id, "found", false);
        }

        Map<String, Object> order = new HashMap<>(rows.get(0));
        String sku = (String) order.get("sku");

        Map<String, Object> stock = inventoryClient.getStock(sku);
        order.put("stock", stock.getOrDefault("stock", 0));

        redis.opsForValue().set(cacheKey, order.toString(), Duration.ofSeconds(30));
        return order;
    }
}
