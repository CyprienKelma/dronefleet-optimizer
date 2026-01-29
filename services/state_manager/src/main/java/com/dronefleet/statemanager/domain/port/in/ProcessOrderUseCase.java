package com.dronefleet.statemanager.domain.port.in;

import com.dronefleet.shared.models.Order;

public interface ProcessOrderUseCase {

    /**
     * Process an order.
     *
     * @param order the order to process
     */
    void processOrder(Order order);
}
