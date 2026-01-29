package com.dronefleet.shared.models;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptimizationSnapshot {
    List<Drone> drones;
    List<Order> orders;
    List<Warehouse> warehouses;
    String sessionId;
    Instant timestamp;
}
