package com.dronefleet.statemanager.domain.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.dronefleet.shared.models.Drone;
import com.dronefleet.shared.models.DroneStatus;
import com.dronefleet.shared.models.Mission;
import com.dronefleet.shared.models.Order;
import com.dronefleet.shared.models.OrderStatus;
import com.dronefleet.shared.models.Waypoint;
import com.dronefleet.shared.models.WaypointType;
import com.dronefleet.statemanager.application.dto.MissionAssignmentDto;
import com.dronefleet.statemanager.domain.exception.BusinessRejectionException;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort.MissionAssignmentResult;

/**
 * Pure domain component that encodes the rules for mission assignment. This class is intended to be
 * called within a transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionAssignmentPolicy {

    private final DronePolicy dronePolicy;

    /**
     * Executes the business logic for assigning a drone to multiple orders.
     *
     * @param drone current drone state from repository (inside transaction)
     * @param orders current orders state from repository (inside transaction)
     * @param dto the mission assignment details from optimizer
     * @return the result containing the new mission and updated entities
     */
    public MissionAssignmentResult computeAssignment(
            Drone drone, List<Order> orders, MissionAssignmentDto dto) {

        // Validation
        if (!dronePolicy.canAcceptMission(drone.getStatus())) {
            throw new BusinessRejectionException(
                    "Drone " + drone.getId() + " is not available. Status: " + drone.getStatus());
        }

        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.ORDER_STATUS_PENDING) {
                throw new BusinessRejectionException(
                        "Order "
                                + order.getId()
                                + " is not in PENDING status. Status: "
                                + order.getStatus());
            }
        }

        // Create Mission
        final String missionId = UUID.randomUUID().toString();

        List<Waypoint> waypoints =
                dto.route().stream()
                        .map(
                                w -> {
                                    WaypointType waypointType;
                                    try {
                                        waypointType = WaypointType.valueOf(w.type());
                                    } catch (IllegalArgumentException | NullPointerException ex) {
                                        throw new BusinessRejectionException(
                                                "Invalid WaypointType: " + w.type());
                                    }
                                    return Waypoint.newBuilder()
                                            .setType(waypointType)
                                            .setPosition(
                                                    com.dronefleet.shared.models.Position
                                                            .newBuilder()
                                                            .setLat(w.position().lat())
                                                            .setLon(w.position().lon())
                                                            .build())
                                            .setRelatedOrderId(
                                                    w.relatedOrderId() != null
                                                            ? w.relatedOrderId()
                                                            : "")
                                            .setRelatedWarehouseId(
                                                    w.relatedWarehouseId() != null
                                                            ? w.relatedWarehouseId()
                                                            : "")
                                            .build();
                                })
                        .collect(Collectors.toList());

        // use same current instant time to avoid mismatched time :
        Instant now = Instant.now();

        final Mission mission =
                Mission.newBuilder()
                        .setId(missionId)
                        .setDroneId(drone.getId())
                        .addAllOrderIds(
                                orders.stream().map(Order::getId).collect(Collectors.toList()))
                        .addAllRoute(waypoints)
                        .setStatus("ACTIVE")
                        .setStartTime(
                                com.google.protobuf.Timestamp.newBuilder()
                                        .setSeconds(now.getEpochSecond())
                                        .setNanos(now.getNano())
                                        .build())
                        .setEstimatedBatteryConsumption(dto.estimatedBatteryConsumption())
                        .setEstimatedDurationMinutes(dto.estimatedDurationMinutes())
                        .build();

        // Update Drone (Immutable)
        Drone updatedDrone =
                drone.toBuilder()
                        .setStatus(DroneStatus.DRONE_STATUS_MOVING)
                        .setCurrentMissionId(missionId)
                        .build();

        // Update Orders (Immutable)
        List<Order> updatedOrders =
                orders.stream()
                        .map(
                                order ->
                                        order.toBuilder()
                                                .setStatus(OrderStatus.ORDER_STATUS_ASSIGNED)
                                                .setAssignedDroneId(drone.getId())
                                                .setAssignedMissionId(missionId)
                                                .build())
                        .collect(Collectors.toList());

        return new MissionAssignmentResult(mission, updatedDrone, updatedOrders);
    }
}
