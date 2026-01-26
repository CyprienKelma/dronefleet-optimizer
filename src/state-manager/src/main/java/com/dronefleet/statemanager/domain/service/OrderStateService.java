package com.dronefleet.statemanager.domain.service;

import com.dronefleet.statemanager.domain.port.in.ProcessOrderUseCase;
import com.dronefleet.statemanager.domain.port.out.OrderRepository;
import com.dronefleet.statemanager.domain.model.Order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class OrderStateService implements ProcessOrderUseCase {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderStateService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void processOrder(Order order) {
        log.info("Processing order {}", order.getId());
        orderRepository.save(order);
        log.info("Order {} processed", order.getId());
    }
}
