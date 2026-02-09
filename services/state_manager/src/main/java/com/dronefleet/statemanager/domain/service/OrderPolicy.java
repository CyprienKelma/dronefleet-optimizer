package com.dronefleet.statemanager.domain.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.dronefleet.shared.models.Order;
import com.dronefleet.shared.models.OrderStatus;

@Component
public class OrderPolicy {

    /** Calculate delivery deadline based on priority. */
    public Instant getDeliveryDeadline(Order order) {
        Instant createdAt =
                order.hasCreatedAt()
                        ? Instant.ofEpochSecond(
                                order.getCreatedAt().getSeconds(), order.getCreatedAt().getNanos())
                        : Instant.now();

        return switch (order.getPriority()) {
            case ORDER_PRIORITY_CRITICAL -> createdAt.plus(15, ChronoUnit.MINUTES);
            case ORDER_PRIORITY_HIGH -> createdAt.plus(30, ChronoUnit.MINUTES);
            case ORDER_PRIORITY_STANDARD -> createdAt.plus(60, ChronoUnit.MINUTES);
            default -> createdAt.plus(60, ChronoUnit.MINUTES);
        };
    }

    /** Calculate if order is overdue. */
    public boolean isOverdue(Order order, Instant now) {
        return now.isAfter(getDeliveryDeadline(order));
    }

    public OrderStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty()) {
            return OrderStatus.ORDER_STATUS_PENDING;
        }
        try {
            if (!statusStr.startsWith("ORDER_STATUS_")) {
                statusStr = "ORDER_STATUS_" + statusStr.toUpperCase();
            }
            return OrderStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OrderStatus.ORDER_STATUS_PENDING;
        }
    }
}
