package com.demo.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "inventory", url = "${client.inventory.url:http://localhost:8082}")
public interface InventoryClient {

    @GetMapping("/api/inventory/{sku}")
    Map<String, Object> getStock(@PathVariable("sku") String sku);
}
