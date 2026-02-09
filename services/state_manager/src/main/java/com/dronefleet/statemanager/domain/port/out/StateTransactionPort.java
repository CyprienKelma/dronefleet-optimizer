package com.dronefleet.statemanager.domain.port.out;

import java.util.List;
import java.util.function.Function;

import com.dronefleet.shared.models.Drone;
import com.dronefleet.shared.models.DroneTelemetry;
import com.dronefleet.shared.models.Mission;
import com.dronefleet.shared.models.OptimizationSnapshot;
import com.dronefleet.shared.models.Order;

/**
 * Port for atomic state transitions in the domain. This decouples the domain from the underlying
 * transaction mechanism (e.g. Firestore transactions).
 */
public interface StateTransactionPort {

    /** Atomically processes a mission assignment for multiple orders. */
    Mission runMissionAssignmentTransaction(
            String droneId,
            List<String> orderIds,
            Function<DroneOrdersContext, MissionAssignmentResult> assignmentLogic);

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

    /** Context for drone and orders during a mission assignment transaction. */
    record DroneOrdersContext(Drone drone, List<Order> orders) {}

    /** Result of the mission assignment logic. */
    record MissionAssignmentResult(
            Mission mission, Drone updatedDrone, List<Order> updatedOrders) {}

    OptimizationSnapshot runSnapshotAcquisitionTransaction(String sessionId, int minBatteryPercent);
}
