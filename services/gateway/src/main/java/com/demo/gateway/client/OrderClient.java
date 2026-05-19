package com.demo.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "order", url = "${client.order.url:http://localhost:8081}")
public interface OrderClient {

    @GetMapping("/api/order/{id}")
    Map<String, Object> getOrder(@PathVariable("id") Long id);
}
