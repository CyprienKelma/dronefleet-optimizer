package com.dronefleet.statemanager.domain.port.out;

import java.util.function.Function;

import com.dronefleet.statemanager.domain.model.Drone;
import com.dronefleet.statemanager.domain.model.DroneTelemetry;
import com.dronefleet.statemanager.domain.model.Mission;
import com.dronefleet.statemanager.domain.model.Order;

/**
 * Port for atomic state transitions in the domain. This decouples the domain from the underlying
 * transaction mechanism (e.g. Firestore transactions).
 */
public interface StateTransactionPort {

    /**
     * Atomically processes a mission assignment.
     *
     * @param droneId The ID of the drone to assign.
     * @param orderId The ID of the order to fulfill.
     * @param assignmentLogic A function that takes the current drone and order, validates them, and
     *     returns a mission if valid.
     * @return The created mission.
     */
    Mission runMissionAssignmentTransaction(
            String droneId,
            String orderId,
            Function<DroneOrderContext, MissionAssignmentResult> assignmentLogic);

    /**
     * Atomically updates a drone state from telemetry, handling ordering and race conditions.
     *
     * @param telemetry The telemetry update to apply.
     */
    void runTelemetryUpdateTransaction(DroneTelemetry telemetry);

    /**
     * Atomically ingests a new order if it doesn't already exist or isn't processed.
     *
     * @param order The order to ingest.
     */
    void runOrderIngestionTransaction(Order order);

    /** Context for drone and order during a mission assignment transaction. */
    record DroneOrderContext(Drone drone, Order order) {}

    /** Result of the mission assignment logic. */
    record MissionAssignmentResult(Mission mission, Drone updatedDrone, Order updatedOrder) {}
}
