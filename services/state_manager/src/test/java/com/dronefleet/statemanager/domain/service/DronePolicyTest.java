package com.dronefleet.statemanager.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.dronefleet.shared.models.Drone;
import com.dronefleet.shared.models.DroneStatus;
import com.dronefleet.shared.models.Position;

class DronePolicyTest {

    private final DronePolicy dronePolicy = new DronePolicy();

    @Test
    void shouldUpdateTelemetryAndPreserveMissionIdIfNullInIncoming() {
        Drone drone =
                Drone.newBuilder()
                        .setId("D1")
                        .setStatus(DroneStatus.DRONE_STATUS_MOVING)
                        .setCurrentMissionId("M1")
                        .build();

        Position newPos = Position.newBuilder().setLat(10.0).setLon(20.0).build();
        Instant now = Instant.now();

        Drone updated =
                dronePolicy.applyTelemetryUpdate(
                        drone, newPos, 80.0, 50.0, DroneStatus.DRONE_STATUS_MOVING, null, now);

        assertEquals(newPos, updated.getPosition());
        assertEquals("M1", updated.getCurrentMissionId());
        assertEquals(now.getEpochSecond(), updated.getLastUpdate().getSeconds());
    }

    @Test
    void shouldClearMissionIdIfStatusIsIdle() {
        Drone drone =
                Drone.newBuilder()
                        .setId("D1")
                        .setStatus(DroneStatus.DRONE_STATUS_MOVING)
                        .setCurrentMissionId("M1")
                        .build();

        Drone updated =
                dronePolicy.applyTelemetryUpdate(
                        drone,
                        Position.newBuilder().setLat(0).setLon(0).build(),
                        100.0,
                        0.0,
                        DroneStatus.DRONE_STATUS_IDLE,
                        null,
                        Instant.now());

        assertEquals("", updated.getCurrentMissionId());
        assertEquals(DroneStatus.DRONE_STATUS_IDLE, updated.getStatus());
    }

    @Test
    void shouldTriggerEmergencyIfBatteryTooLow() {
        Drone drone =
                Drone.newBuilder()
                        .setId("D1")
                        .setStatus(DroneStatus.DRONE_STATUS_MOVING)
                        .setBatteryPercentage(10.0)
                        .build();

        Drone updated =
                dronePolicy.applyTelemetryUpdate(
                        drone,
                        Position.newBuilder().setLat(0).setLon(0).build(),
                        4.0,
                        0.0,
                        DroneStatus.DRONE_STATUS_MOVING,
                        null,
                        Instant.now());

        assertEquals(DroneStatus.DRONE_STATUS_EMERGENCY, updated.getStatus());
    }
}
