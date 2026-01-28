package com.dronefleet.statemanager.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class OptimizationSnapshot {
    List<Drone> drones;
    List<Order> orders;
    List<Warehouse> warehouses;
    String sessionId;
    Instant timestamp;
}
