package com.dronefleet.statemanager.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record OptimizationSnapshotDto(
    @JsonProperty("session_id") String sessionId,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("drones") List<DroneSnapshotDto> drones,
    @JsonProperty("orders") List<OrderSnapshotDto> orders,
    @JsonProperty("warehouses") List<WarehouseSnapshotDto> warehouses
) {
    public record DroneSnapshotDto(
        String id,
        PositionDto position,
        @JsonProperty("battery_percentage") double batteryPercentage,
        @JsonProperty("status") String status,
        @JsonProperty("home_depot_id") String homeDepotId
    ) {}

    public record OrderSnapshotDto(
        String id,
        @JsonProperty("pickup_location") PositionDto pickupLocation,
        @JsonProperty("delivery_location") PositionDto deliveryLocation,
        String priority,
        @JsonProperty("product_type") String productType
    ) {}
    public record WarehouseSnapshotDto(
        String id,
        String name,
        PositionDto position,
        @JsonProperty("authorized_product_types") List<String> authorizedProductTypes,
        @JsonProperty("is_cold_storage_capable") boolean isColdStorageCapable
    ) {}

    public record PositionDto(double lat, double lon) {}
}
