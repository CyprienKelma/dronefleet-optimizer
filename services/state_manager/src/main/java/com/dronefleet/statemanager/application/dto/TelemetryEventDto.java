package com.dronefleet.statemanager.application.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/** DTO for incoming telemetry events from Pub/Sub. */
public record TelemetryEventDto(
        @JsonProperty("drone_id") String droneId,
        @JsonProperty("timestamp") OffsetDateTime timestamp,
        @JsonProperty("position") GeoPointDto position,
        @JsonProperty("battery_percentage") double batteryPercentage,
        @JsonProperty("speed_kmh") double speedKmh,
        @JsonProperty("status") String status,
        @JsonProperty("current_mission_id") String currentMissionId) {
    public record GeoPointDto(double lat, double lon) {}
}
