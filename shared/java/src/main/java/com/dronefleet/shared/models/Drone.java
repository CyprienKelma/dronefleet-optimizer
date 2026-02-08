package com.dronefleet.shared.models;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Drone {
    private String id;
    private Position position;
    private double batteryPercentage;
    private double speedKmh;
    private DroneStatus status;
    private String currentMissionId;
    private Instant lastUpdate;
    private String solvingSessionId;
    private String homeDepotId;

    // Battery calculation metadata
    private int batteryCapacityMah;
    private double consumptionPerKm; // % per km
    private int maxFlightTimeMinutes;

    public boolean isAvailable() {
        return status == DroneStatus.IDLE && batteryPercentage > 20.0;
    }

    /**
     * Calculate if drone can complete a route and return home.
     * @param routeDistanceKm Total distance of planned route in km
     * @param safetyMargin Safety margin percentage (ex: 1.2 = 20% margin)
     * @return true if battery sufficient
     */
    public boolean canCompleteRoute(double routeDistanceKm, double safetyMargin) {
        double estimatedConsumption = routeDistanceKm * consumptionPerKm * safetyMargin;
        double remainingAfterRoute = batteryPercentage - estimatedConsumption;
        return remainingAfterRoute >= 20.0; // Min 20% reserve
    }

    // Business rule: Update telemetry and handle low battery status
    public void updateTelemetry(
            Position position,
            double batteryPercentage,
            double speedKmh,
            DroneStatus status,
            String incomingMissionId,
            Instant updateTime) {
        this.position = position;
        this.batteryPercentage = batteryPercentage;
        this.speedKmh = speedKmh;
        this.status = status;

        // Don't clear currentMissionId if the incoming one is null but we already have one
        // and the drone is still in a status that implies it might be on a mission (like MOVING)
        if (incomingMissionId != null) {
            this.currentMissionId = incomingMissionId;
        } else if (this.status == DroneStatus.IDLE || this.status == DroneStatus.CHARGING) {
            // Only clear it if the drone says it's IDLE or CHARGING
            this.currentMissionId = null;
        }

        this.lastUpdate = updateTime != null ? updateTime : Instant.now();

        // Auto-land if battery is critical
        if (this.batteryPercentage < 5.0 && this.status != DroneStatus.EMERGENCY) {
            this.status = DroneStatus.EMERGENCY;
        }
    }
}
