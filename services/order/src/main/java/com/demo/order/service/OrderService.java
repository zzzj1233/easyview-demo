package com.demo.order.service;

import com.demo.api.dto.StockDTO;
import com.demo.api.facade.InventoryFacade;
import com.demo.order.chaos.ChaosState;
import org.apache.dubbo.config.annotation.DubboReference;
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
    private final ChaosState chaos;

    @DubboReference(timeout = 3000, retries = 1, check = false)
    private InventoryFacade inventoryFacade;

    public OrderService(JdbcTemplate jdbc, StringRedisTemplate redis, ChaosState chaos) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.chaos = chaos;
    }

    public Map<String, Object> getOrder(Long id) {
        chaos.maybeLatency();
        chaos.maybeError();

        String cacheKey = "order:" + id;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) log.debug("cache hit id={}", id);

        long dbExtra = chaos.dbExtraMs.get();
        if (dbExtra > 0) {
            jdbc.queryForObject("SELECT SLEEP(?)", Long.class, dbExtra / 1000.0);
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, sku, qty, status, intent_id, created_at FROM orders WHERE id = ?", id);
        if (rows.isEmpty()) {
            log.warn("order not found id={}", id);
            return Map.of("id", id, "found", false);
        }

        Map<String, Object> order = new HashMap<>(rows.get(0));
        String sku = (String) order.get("sku");

        StockDTO stock = inventoryFacade.getStock(sku);
        order.put("stock", stock != null ? stock.getStock() : 0);

        redis.opsForValue().set(cacheKey, order.toString(), Duration.ofSeconds(30));
        return order;
    }
}
