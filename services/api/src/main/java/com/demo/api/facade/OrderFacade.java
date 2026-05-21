package com.demo.api.facade;

import com.demo.api.dto.OrderDTO;

public interface OrderFacade {
    OrderDTO getOrder(Long id);
}
