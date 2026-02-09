package com.dronefleet.shared.models;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mission {
    private String id;
    private String droneId;
    private List<String> orderIds;
    private List<Waypoint> route;
    private String status; // ACTIVE, COMPLETED, FAILED
    private Instant startTime;
    private Instant endTime;
    private Double estimatedBatteryConsumption;
    private Double estimatedDurationMinutes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Waypoint {
        private WaypointType type;
        private Position position;
        private String relatedOrderId;
        private String relatedWarehouseId;
    }

    public enum WaypointType {
        DEPOT_START,
        WAREHOUSE_PICKUP,
        HOSPITAL_DELIVERY,
        DEPOT_RETURN
    }
}
