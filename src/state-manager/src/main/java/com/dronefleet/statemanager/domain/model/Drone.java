package com.dronefleet.statemanager.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.time.Instant;

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


    public boolean isAvailable() {
        return status == DroneStatus.IDLE && batteryPercentage > 20.0;
    }

    //Business rule: Update telemetry and handle low battery status
    public void updateTelemetry(Position position, double batteryPercentage, double speedKmh, DroneStatus status, String currentMissionId) {
        this.position = position;
        this.batteryPercentage = batteryPercentage;
        this.speedKmh = speedKmh;
        this.status = status;
        this.currentMissionId = currentMissionId;
        this.lastUpdate = Instant.now();

        // Auto-land if battery is critical
        if (this.batteryPercentage < 5.0 && this.status != DroneStatus.EMERGENCY) {
            this.status = DroneStatus.EMERGENCY;
        }
    }
}
