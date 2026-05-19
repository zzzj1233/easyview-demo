package com.demo.gateway.controller;

import com.demo.gateway.client.OrderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {
    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);
    private final OrderClient orderClient;

    public GatewayController(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @GetMapping("/order/{id}")
    public Map<String, Object> getOrder(@PathVariable Long id) {
        log.info("gateway -> order id={}", id);
        return orderClient.getOrder(id);
    }
}
