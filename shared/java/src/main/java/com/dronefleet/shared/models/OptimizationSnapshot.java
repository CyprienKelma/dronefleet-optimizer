package com.dronefleet.shared.models;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptimizationSnapshot {
    private List<Drone> drones;
    private List<Order> orders;
    private List<Warehouse> warehouses;
    private Depot depot;
    private String sessionId;
    private Instant timestamp;
}
