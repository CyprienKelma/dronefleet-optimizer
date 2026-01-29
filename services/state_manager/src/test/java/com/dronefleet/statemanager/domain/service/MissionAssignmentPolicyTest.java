package com.dronefleet.statemanager.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dronefleet.shared.models.Drone;
import com.dronefleet.shared.models.DroneStatus;
import com.dronefleet.shared.models.Order;
import com.dronefleet.shared.models.OrderStatus;
import com.dronefleet.shared.models.Position;
import com.dronefleet.statemanager.domain.exception.BusinessRejectionException;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort.MissionAssignmentResult;

class MissionAssignmentPolicyTest {

    private MissionAssignmentPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new MissionAssignmentPolicy();
    }

    @Test
    void shouldCreateMissionWhenDroneAndOrderAreAvailable() {
        Drone drone =
                Drone.builder().id("D1").status(DroneStatus.IDLE).batteryPercentage(100.0).build();

        Order order = Order.builder().id("O1").status(OrderStatus.PENDING).build();

        List<Position> route = List.of(new Position(1.0, 1.0));

        MissionAssignmentResult result = policy.computeAssignment(drone, order, route);

        assertNotNull(result.mission());
        assertEquals("D1", result.mission().getDroneId());
        assertEquals("O1", result.mission().getOrderId());
        assertEquals(DroneStatus.MOVING, result.updatedDrone().getStatus());
        assertEquals(OrderStatus.ASSIGNED, result.updatedOrder().getStatus());
        assertEquals(result.mission().getId(), result.updatedOrder().getAssignedMissionId());
    }

    @Test
    void shouldBeIdempotentIfAlreadyAssignedToSameDrone() {
        Drone drone =
                Drone.builder().id("D1").status(DroneStatus.MOVING).currentMissionId("M1").build();

        Order order =
                Order.builder()
                        .id("O1")
                        .status(OrderStatus.ASSIGNED)
                        .assignedDroneId("D1")
                        .assignedMissionId("M1")
                        .build();

        List<Position> route = List.of(new Position(1.0, 1.0));

        MissionAssignmentResult result = policy.computeAssignment(drone, order, route);

        assertNull(result.mission());
        assertEquals(drone, result.updatedDrone());
        assertEquals(order, result.updatedOrder());
    }

    @Test
    void shouldRejectIfDroneIsNotAvailable() {
        Drone drone = Drone.builder().id("D1").status(DroneStatus.MOVING).build();

        Order order = Order.builder().id("O1").status(OrderStatus.PENDING).build();

        assertThrows(
                BusinessRejectionException.class,
                () -> policy.computeAssignment(drone, order, List.of()));
    }

    @Test
    void shouldRejectIfOrderIsNotPending() {
        Drone drone =
                Drone.builder().id("D1").status(DroneStatus.IDLE).batteryPercentage(100.0).build();

        Order order =
                Order.builder()
                        .id("O1")
                        .status(OrderStatus.ASSIGNED)
                        .assignedDroneId("D2") // assigned to someone else
                        .build();

        assertThrows(
                BusinessRejectionException.class,
                () -> policy.computeAssignment(drone, order, List.of()));
    }
}
