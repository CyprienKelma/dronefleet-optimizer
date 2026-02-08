package com.dronefleet.shared.models;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String id;
    private Position pickupLocation;
    private Position deliveryLocation;
    private OrderStatus status;
    private OrderPriority priority;
    private String productType;
    private Instant createdAt;
    private String assignedDroneId;
    private String assignedMissionId;
    private String solvingSessionId;

    /**
     * Calculate delivery deadline based on priority.
     */
    public Instant getDeliveryDeadline() {
        if (createdAt == null) return Instant.now().plus(60, ChronoUnit.MINUTES);
        return switch (priority) {
            case CRITICAL -> createdAt.plus(15, ChronoUnit.MINUTES);
            case HIGH -> createdAt.plus(30, ChronoUnit.MINUTES);
            case STANDARD -> createdAt.plus(60, ChronoUnit.MINUTES);
            default -> createdAt.plus(60, ChronoUnit.MINUTES);
        };
    }

    /**
     * Calculate if order is overdue.
     */
    public boolean isOverdue(Instant now) {
        return now.isAfter(getDeliveryDeadline());
    }
}
