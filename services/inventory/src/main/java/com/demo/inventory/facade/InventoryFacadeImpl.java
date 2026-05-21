package com.demo.inventory.facade;

import com.demo.api.dto.StockDTO;
import com.demo.api.facade.InventoryFacade;
import com.demo.inventory.service.InventoryService;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Map;

@DubboService
public class InventoryFacadeImpl implements InventoryFacade {
    private final InventoryService service;
    public InventoryFacadeImpl(InventoryService service) { this.service = service; }

    @Override
    public StockDTO getStock(String sku) {
        Map<String, Object> row = service.getStock(sku);
        Object stockObj = row.getOrDefault("stock", 0);
        int stock = stockObj instanceof Number ? ((Number) stockObj).intValue() : 0;
        return new StockDTO(sku, stock);
    }
}
