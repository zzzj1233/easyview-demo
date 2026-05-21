package com.demo.inventory.service;

import com.demo.inventory.chaos.ChaosState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InventoryService {

    private final JdbcTemplate jdbc;
    private final ChaosState chaos;

    public InventoryService(JdbcTemplate jdbc, ChaosState chaos) {
        this.jdbc = jdbc;
        this.chaos = chaos;
    }

    public Map<String, Object> getStock(String sku) {
        chaos.maybeLatency();
        chaos.maybeError();
        long dbExtra = chaos.dbExtraMs.get();
        if (dbExtra > 0) {
            jdbc.queryForObject("SELECT SLEEP(?)", Long.class, dbExtra / 1000.0);
        }
        return jdbc.queryForList(
                "SELECT sku, stock, updated_at FROM inventory WHERE sku = ?",
                sku
        ).stream().findFirst().orElse(Map.of("sku", sku, "stock", 0));
    }
}
