package com.demo.gateway.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.demo.api.dto.OrderDTO;

public class SentinelHandlers {
    public static OrderDTO onBlock(Long id, BlockException ex) {
        OrderDTO dto = new OrderDTO();
        dto.setId(id); dto.setSku("BLOCKED"); dto.setQty(0); dto.setStock(0);
        return dto;
    }
    public static OrderDTO onFallback(Long id, Throwable t) {
        OrderDTO dto = new OrderDTO();
        dto.setId(id); dto.setSku("FALLBACK"); dto.setQty(0); dto.setStock(0);
        return dto;
    }
}
