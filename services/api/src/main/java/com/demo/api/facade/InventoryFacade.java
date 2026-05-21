package com.demo.api.facade;

import com.demo.api.dto.StockDTO;

public interface InventoryFacade {
    StockDTO getStock(String sku);
}
