package com.dronefleet.statemanager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private String id;
    private Position pickupLocation;
    private Position deliveryLocation;
    private String status; // PENDING, ASSIGNED, DELIVERED, CANCELLED
    private String priority; // STANDARD, HIGH, CRITICAL
    private Instant createdAt;
    private String assignedDroneId;
    private String assignedMissionId;
}
