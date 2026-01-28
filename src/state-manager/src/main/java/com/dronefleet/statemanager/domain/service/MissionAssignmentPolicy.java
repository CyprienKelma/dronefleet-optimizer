package com.dronefleet.statemanager.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.dronefleet.statemanager.domain.exception.BusinessRejectionException;
import com.dronefleet.statemanager.domain.model.Drone;
import com.dronefleet.statemanager.domain.model.DroneStatus;
import com.dronefleet.statemanager.domain.model.Mission;
import com.dronefleet.statemanager.domain.model.Order;
import com.dronefleet.statemanager.domain.model.OrderStatus;
import com.dronefleet.statemanager.domain.model.Position;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort.MissionAssignmentResult;

/**
 * Pure domain component that encodes the rules for mission assignment. This class is intended to be
 * called within a transaction.
 */
@Slf4j
@Component
public class MissionAssignmentPolicy {

    /**
     * Executes the business logic for assigning a drone to an order.
     *
     * @param drone current drone state from repository (inside transaction)
     * @param order current order state from repository (inside transaction)
     * @param route planned route
     * @return the result containing the new mission and updated entities
     */
    public MissionAssignmentResult computeAssignment(
            Drone drone, Order order, List<Position> route) {
        // Idempotency check: if order is already assigned to this drone, return current state
        if (order.getStatus() == OrderStatus.ASSIGNED && drone.getId().equals(order.getAssignedDroneId())) {
            log.info("Order {} already assigned to drone {}. Skipping mission creation (idempotent).",
                    order.getId(), drone.getId());
            return new MissionAssignmentResult(null, drone, order);
        }

        // Validation: Optimizer locks drones as RESERVED and orders as SOLVING
        if (drone.getStatus() != DroneStatus.IDLE && drone.getStatus() != DroneStatus.RESERVED) {
            throw new BusinessRejectionException("Drone " + drone.getId() + " is not available. Status: " + drone.getStatus());
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.SOLVING) {
            throw new BusinessRejectionException("Order " + order.getId() + " is not in PENDING or SOLVING status. Status: " + order.getStatus());
        }

        // Create Mission
        final String missionId = UUID.randomUUID().toString();
        final Mission mission =
                Mission.builder()
                        .id(missionId)
                        .droneId(drone.getId())
                        .orderId(order.getId())
                        .route(route)
                        .status("ACTIVE")
                        .startTime(Instant.now())
                        .build();

        // Update Drone
        drone.setStatus(DroneStatus.MOVING);
        drone.setCurrentMissionId(missionId);
        drone.setSolvingSessionId(null); // Clear session ID once assigned

        // Update Order
        order.setStatus(OrderStatus.ASSIGNED);
        order.setAssignedDroneId(drone.getId());
        order.setAssignedMissionId(missionId);
        order.setSolvingSessionId(null); // Clear session ID once assigned

        return new MissionAssignmentResult(mission, drone, order);
    }
}
