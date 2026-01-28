package com.dronefleet.statemanager.domain.model;

import java.time.Instant;

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
    private String priority;
    private Instant createdAt;
    private String assignedDroneId;
    private String assignedMissionId;
    private String solvingSessionId;
}
