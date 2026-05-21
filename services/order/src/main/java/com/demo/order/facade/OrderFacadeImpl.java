package com.demo.order.facade;

import com.demo.api.dto.OrderDTO;
import com.demo.api.facade.OrderFacade;
import com.demo.order.service.OrderService;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.HashMap;
import java.util.Map;

@DubboService
public class OrderFacadeImpl implements OrderFacade {
    private final OrderService service;
    public OrderFacadeImpl(OrderService service) { this.service = service; }

    @Override
    public OrderDTO getOrder(Long id) {
        Map<String, Object> raw = service.getOrder(id);
        OrderDTO dto = new OrderDTO();
        dto.setId(asLong(raw.get("id")));
        dto.setSku((String) raw.get("sku"));
        dto.setQty(asInt(raw.get("qty")));
        dto.setStock(asInt(raw.get("stock")));
        Map<String, Object> extra = new HashMap<>(raw);
        extra.keySet().removeAll(java.util.List.of("id", "sku", "qty", "stock"));
        dto.setExtra(extra);
        return dto;
    }

    private static Long asLong(Object v) { return v instanceof Number ? ((Number) v).longValue() : null; }
    private static Integer asInt(Object v) { return v instanceof Number ? ((Number) v).intValue() : null; }
}
