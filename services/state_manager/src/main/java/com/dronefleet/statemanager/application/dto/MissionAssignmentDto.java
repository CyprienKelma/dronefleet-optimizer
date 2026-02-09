package com.dronefleet.statemanager.application.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** DTO for incoming mission assignment decisions from the Optimizer. */
public record MissionAssignmentDto(
        @JsonProperty("drone_id") String droneId,
        @JsonProperty("order_ids") List<String> orderIds,
        @JsonProperty("route") List<WaypointDto> route,
        @JsonProperty("estimated_battery_consumption") double estimatedBatteryConsumption,
        @JsonProperty("estimated_duration_minutes") double estimatedDurationMinutes) {

    public record WaypointDto(
            @JsonProperty("type") String type,
            @JsonProperty("position") GeoPointDto position,
            @JsonProperty("related_order_id") String relatedOrderId,
            @JsonProperty("related_warehouse_id") String relatedWarehouseId) {}

    public record GeoPointDto(double lat, double lon) {}
}
