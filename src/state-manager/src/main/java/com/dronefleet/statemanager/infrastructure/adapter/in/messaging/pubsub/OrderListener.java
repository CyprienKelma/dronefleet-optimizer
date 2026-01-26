package com.dronefleet.statemanager.infrastructure.adapter.in.messaging.pubsub;

import com.dronefleet.statemanager.application.dto.OrderEventDto;
import com.dronefleet.statemanager.domain.model.Order;
import com.dronefleet.statemanager.domain.model.Position;
import com.dronefleet.statemanager.domain.port.in.ProcessOrderUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;
import com.dronefleet.statemanager.domain.exception.BusinessRejectionException;
/**
 * Inbound adapter that listens to Pub/Sub messages for orders and routes them to the domain.
 */
@Slf4j
@Component
public class OrderListener {

    private final ProcessOrderUseCase processOrderUseCase;
    private final ObjectMapper objectMapper;

    public OrderListener(ProcessOrderUseCase processOrderUseCase, ObjectMapper objectMapper) {
        this.processOrderUseCase = processOrderUseCase;
        this.objectMapper = objectMapper;
    }

    @ServiceActivator(inputChannel = "ordersInputChannel")
    public void handleOrderMessage(String payload) {
        try {
            log.debug("Received order payload: {}", payload);
            OrderEventDto dto = objectMapper.readValue(payload, OrderEventDto.class);

            // mapping DTO -> domain model
            Order order = Order.builder()
                .id(dto.orderId())
                .pickupLocation(new Position(dto.pickupLocation().lat(), dto.pickupLocation().lon()))
                .deliveryLocation(new Position(dto.dropoffLocation().lat(), dto.dropoffLocation().lon()))
                .priority(dto.priority())
                .createdAt(dto.createdAt() != null ? dto.createdAt().toInstant() : null)
                .build();

            processOrderUseCase.processOrder(order);
            log.info("Successfully ingested order {}", dto.orderId());
        } catch (BusinessRejectionException e) {
            log.warn("Order ingestion rejected: {}", e.getMessage());
            // Business rejection should not be retried as it's a "permanent" failure of this order
        } catch (Exception e) {
            log.error("Transient error processing order message: {}. Will be retried.", e.getMessage(), e);
            throw new RuntimeException("Error processing message", e); // Throwing re-triggers retry in Spring Cloud GCP
        }
    }
}
