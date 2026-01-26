package com.dronefleet.statemanager.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class DroneTest {

    @Test
    void shouldUpdateTelemetryAndPreserveMissionIdIfNullInIncoming() {
        Drone drone =
                Drone.builder().id("D1").status(DroneStatus.MOVING).currentMissionId("M1").build();

        Position newPos = new Position(10.0, 20.0);
        Instant now = Instant.now();

        drone.updateTelemetry(newPos, 80.0, 50.0, DroneStatus.MOVING, null, now);

        assertEquals(newPos, drone.getPosition());
        assertEquals("M1", drone.getCurrentMissionId());
        assertEquals(now, drone.getLastUpdate());
    }

    @Test
    void shouldClearMissionIdIfStatusIsIdle() {
        Drone drone =
                Drone.builder().id("D1").status(DroneStatus.MOVING).currentMissionId("M1").build();

        drone.updateTelemetry(
                new Position(0, 0), 100.0, 0.0, DroneStatus.IDLE, null, Instant.now());

        assertNull(drone.getCurrentMissionId());
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    @Test
    void shouldTriggerEmergencyIfBatteryTooLow() {
        Drone drone =
                Drone.builder().id("D1").status(DroneStatus.MOVING).batteryPercentage(10.0).build();

        drone.updateTelemetry(
                new Position(0, 0), 4.0, 0.0, DroneStatus.MOVING, null, Instant.now());

        assertEquals(DroneStatus.EMERGENCY, drone.getStatus());
    }
}
