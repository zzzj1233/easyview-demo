package com.demo.api.dto;

import java.io.Serializable;

public class StockDTO implements Serializable {
    private String sku;
    private Integer stock;

    public StockDTO() {}
    public StockDTO(String sku, Integer stock) { this.sku = sku; this.stock = stock; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
