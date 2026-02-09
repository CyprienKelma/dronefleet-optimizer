package com.dronefleet.statemanager.infrastructure.adapter.in.messaging.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

import com.dronefleet.shared.models.Order;
import com.dronefleet.shared.models.OrderPriority;
import com.dronefleet.shared.models.Position;
import com.dronefleet.statemanager.application.dto.OrderEventDto;
import com.dronefleet.statemanager.domain.exception.BusinessRejectionException;
import com.dronefleet.statemanager.domain.port.in.ProcessOrderUseCase;

/** Inbound adapter that listens to Pub/Sub messages for orders and routes them to the domain. */
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

            // mapping DTO -> domain model (Immutable Builder)
            Order.Builder builder =
                    Order.newBuilder()
                            .setId(dto.orderId())
                            .setPickupLocation(
                                    Position.newBuilder()
                                            .setLat(dto.pickupLocation().lat())
                                            .setLon(dto.pickupLocation().lon())
                                            .build())
                            .setDeliveryLocation(
                                    Position.newBuilder()
                                            .setLat(dto.dropoffLocation().lat())
                                            .setLon(dto.dropoffLocation().lon())
                                            .build())
                            .setPriority(parsePriority(dto.priority()))
                            .setProductType(dto.productType() != null ? dto.productType() : "");

            if (dto.createdAt() != null) {
                builder.setCreatedAt(
                        com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(dto.createdAt().toEpochSecond())
                                .setNanos(dto.createdAt().getNano())
                                .build());
            }

            processOrderUseCase.processOrder(builder.build());
            log.info("Successfully ingested order {}", dto.orderId());
        } catch (BusinessRejectionException e) {
            log.warn("Order ingestion rejected: {}", e.getMessage());
        } catch (Exception e) {
            log.error(
                    "Transient error processing order message: {}. Will be retried.",
                    e.getMessage(),
                    e);
            throw new RuntimeException("Error processing message", e);
        }
    }

    private OrderPriority parsePriority(String priority) {
        if (priority == null) {
            return OrderPriority.ORDER_PRIORITY_STANDARD;
        }
        try {
            return OrderPriority.valueOf("ORDER_PRIORITY_" + priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown priority '{}', defaulting to STANDARD", priority);
            return OrderPriority.ORDER_PRIORITY_STANDARD;
        }
    }
}
