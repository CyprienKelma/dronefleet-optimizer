package com.dronefleet.statemanager.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dronefleet.statemanager.domain.model.Order;
import com.dronefleet.statemanager.domain.port.in.ProcessOrderUseCase;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort;

@Slf4j
@Service
public class OrderStateService implements ProcessOrderUseCase {

    private final StateTransactionPort transactionPort;

    @Autowired
    public OrderStateService(StateTransactionPort transactionPort) {
        this.transactionPort = transactionPort;
    }

    @Override
    public void processOrder(Order order) {
        log.info("Requesting atomic ingestion for order {}", order.getId());
        transactionPort.runOrderIngestionTransaction(order);
    }
}
