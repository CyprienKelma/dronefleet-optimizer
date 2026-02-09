package com.dronefleet.statemanager.domain.service;

import java.time.Instant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.dronefleet.shared.models.Drone;
import com.dronefleet.shared.models.DroneStatus;
import com.dronefleet.shared.models.Position;

@Slf4j
@Component
public class DronePolicy {

    public boolean isAvailable(Drone drone) {
        return drone.getStatus() == DroneStatus.DRONE_STATUS_IDLE
                && drone.getBatteryPercentage() > 20.0;
    }

    /**
     * Calculate if drone can complete a route and return home.
     *
     * @param drone the drone to check
     * @param routeDistanceKm Total distance of planned route in km
     * @param safetyMargin Safety margin percentage (ex: 1.2 = 20% margin)
     * @return true if battery sufficient
     */
    public boolean canCompleteRoute(Drone drone, double routeDistanceKm, double safetyMargin) {
        double estimatedConsumption = routeDistanceKm * drone.getConsumptionPerKm() * safetyMargin;
        double remainingAfterRoute = drone.getBatteryPercentage() - estimatedConsumption;
        return remainingAfterRoute >= 20.0; // Min 20% reserve
    }

    /**
     * Executes the business logic for updating a drone with new telemetry.
     *
     * @param drone the current drone state
     * @param position new position
     * @param batteryPercentage new battery level
     * @param speedKmh new speed
     * @param status new status
     * @param incomingMissionId new mission ID if any
     * @param updateTime time of update
     * @return the updated drone
     */
    public Drone applyTelemetryUpdate(
            Drone drone,
            Position position,
            double batteryPercentage,
            double speedKmh,
            DroneStatus status,
            String incomingMissionId,
            Instant updateTime) {

        Drone.Builder builder =
                drone.toBuilder()
                        .setPosition(position)
                        .setBatteryPercentage(batteryPercentage)
                        .setSpeedKmh(speedKmh)
                        .setStatus(status);

        if (incomingMissionId != null && !incomingMissionId.isEmpty()) {
            builder.setCurrentMissionId(incomingMissionId);
        } else if (status == DroneStatus.DRONE_STATUS_IDLE
                || status == DroneStatus.DRONE_STATUS_CHARGING) {
            builder.setCurrentMissionId("");
        }

        Instant lastUpdate = updateTime != null ? updateTime : Instant.now();
        builder.setLastUpdate(
                com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(lastUpdate.getEpochSecond())
                        .setNanos(lastUpdate.getNano())
                        .build());

        // Auto-land if battery is critical
        if (batteryPercentage < 5.0 && status != DroneStatus.DRONE_STATUS_EMERGENCY) {
            builder.setStatus(DroneStatus.DRONE_STATUS_EMERGENCY);
        }

        return builder.build();
    }

    public DroneStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty()) {
            return DroneStatus.DRONE_STATUS_UNKNOWN;
        }
        try {
            // Protobuf enum names are usually DRONE_STATUS_IDLE, etc.
            // But we might want to support the short names "IDLE" too.
            if (!statusStr.startsWith("DRONE_STATUS_")) {
                statusStr = "DRONE_STATUS_" + statusStr.toUpperCase();
            }
            return DroneStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Status unknown : '{}', using UNKNOWN by default", statusStr);
            return DroneStatus.DRONE_STATUS_UNKNOWN;
        }
    }

    public boolean isOperational(DroneStatus status) {
        return status == DroneStatus.DRONE_STATUS_IDLE || status == DroneStatus.DRONE_STATUS_MOVING;
    }

    public boolean requiresAttention(DroneStatus status) {
        return status == DroneStatus.DRONE_STATUS_MAINTENANCE
                || status == DroneStatus.DRONE_STATUS_PROBLEM;
    }

    public boolean canAcceptMission(DroneStatus status) {
        return status == DroneStatus.DRONE_STATUS_IDLE;
    }
}
