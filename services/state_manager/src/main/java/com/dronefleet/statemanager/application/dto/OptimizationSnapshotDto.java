package com.dronefleet.statemanager.application.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OptimizationSnapshotDto(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("depot") DepotDto depot,
        @JsonProperty("drones") List<DroneSnapshotDto> drones,
        @JsonProperty("orders") List<OrderSnapshotDto> orders,
        @JsonProperty("warehouses") List<WarehouseSnapshotDto> warehouses) {

    public record DepotDto(String id, String name, PositionDto position) {}

    public record DroneSnapshotDto(
            String id,
            PositionDto position,
            @JsonProperty("battery_percentage") double batteryPercentage,
            @JsonProperty("status") String status,
            @JsonProperty("home_depot_id") String homeDepotId,
            @JsonProperty("consumption_per_km") double consumptionPerKm,
            @JsonProperty("max_flight_time_minutes") int maxFlightTimeMinutes) {}

    public record OrderSnapshotDto(
            String id,
            @JsonProperty("delivery_location") PositionDto deliveryLocation,
            String priority,
            @JsonProperty("product_type") String productType,
            @JsonProperty("created_at") Instant createdAt) {}

    public record WarehouseSnapshotDto(
            String id,
            String name,
            PositionDto position,
            @JsonProperty("authorized_product_types") List<String> authorizedProductTypes,
            @JsonProperty("is_cold_storage_capable") boolean isColdStorageCapable) {}

    public record PositionDto(double lat, double lon) {}
}
