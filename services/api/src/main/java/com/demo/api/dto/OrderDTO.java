package com.demo.api.dto;

import java.io.Serializable;
import java.util.Map;

public class OrderDTO implements Serializable {
    private Long id;
    private String sku;
    private Integer qty;
    private Integer stock;
    private Map<String, Object> extra;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }
}
