package com.dronefleet.statemanager.application.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** DTO for exposing mission data through the REST API. */
public record MissionDto(
        String id,
        @JsonProperty("drone_id") String droneId,
        @JsonProperty("order_ids") List<String> orderIds,
        String status,
        List<WaypointDto> route,
        @JsonProperty("estimated_battery_consumption") double estimatedBatteryConsumption,
        @JsonProperty("estimated_duration_minutes") double estimatedDurationMinutes,
        @JsonProperty("start_time") Instant startTime) {

    public record WaypointDto(
            String type,
            PositionDto position,
            @JsonProperty("related_order_id") String relatedOrderId,
            @JsonProperty("related_warehouse_id") String relatedWarehouseId) {}

    public record PositionDto(double lat, double lon) {}
}
