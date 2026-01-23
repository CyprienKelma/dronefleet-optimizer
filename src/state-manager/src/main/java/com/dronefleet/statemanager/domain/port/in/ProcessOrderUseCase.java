package com.dronefleet.statemanager.domain.port.in;

import com.dronefleet.statemanager.domain.model.Order;

public interface ProcessOrderUseCase {
    void processOrder(Order order);
}
