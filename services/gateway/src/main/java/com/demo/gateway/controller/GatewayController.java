package com.demo.gateway.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.demo.api.dto.OrderDTO;
import com.demo.api.facade.OrderFacade;
import com.demo.gateway.sentinel.SentinelHandlers;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GatewayController {
    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    @DubboReference(timeout = 3000, retries = 1, check = false)
    private OrderFacade orderFacade;

    @GetMapping("/order/{id}")
    @SentinelResource(value = "gw.getOrder",
            blockHandlerClass = SentinelHandlers.class, blockHandler = "onBlock",
            fallbackClass = SentinelHandlers.class, fallback = "onFallback")
    public OrderDTO getOrder(@PathVariable Long id) {
        log.info("gateway -> order id={}", id);
        return orderFacade.getOrder(id);
    }
}
