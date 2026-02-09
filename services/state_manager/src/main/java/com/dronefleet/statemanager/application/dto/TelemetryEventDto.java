package com.dronefleet.statemanager.application.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/** DTO for incoming telemetry events from Pub/Sub. */
public record TelemetryEventDto(
        @JsonProperty("drone_id") @JsonAlias("droneId") String droneId,
        @JsonProperty("timestamp") OffsetDateTime timestamp,
        @JsonProperty("position") GeoPointDto position,
        @JsonProperty("battery_percentage") @JsonAlias("batteryPercentage")
                double batteryPercentage,
        @JsonProperty("speed_kmh") @JsonAlias("speedKmh") double speedKmh,
        @JsonProperty("status") String status,
        @JsonProperty("current_mission_id") @JsonAlias({"currentMissionId", "current_mission_id"})
                String currentMissionId) {
    public record GeoPointDto(double lat, double lon) {}
}
