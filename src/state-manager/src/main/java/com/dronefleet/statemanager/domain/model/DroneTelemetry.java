package com.dronefleet.statemanager.domain.model;

import java.time.OffsetDateTime;

/**
 * Domain model representing drone telemetry data.
 */
public record DroneTelemetry(
    String droneId,
    OffsetDateTime timestamp,
    Position position,
    double batteryPercentage,
    double speedKmh,
    DroneStatus status,
    String currentMissionId
) {
}
