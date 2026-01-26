package com.dronefleet.statemanager.domain.service;

import com.dronefleet.statemanager.domain.exception.BusinessRejectionException;
import com.dronefleet.statemanager.domain.model.Drone;
import com.dronefleet.statemanager.domain.model.DroneStatus;
import com.dronefleet.statemanager.domain.model.Order;
import com.dronefleet.statemanager.domain.model.Position;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort.MissionAssignmentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MissionAssignmentPolicyTest {

    private MissionAssignmentPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new MissionAssignmentPolicy();
    }

    @Test
    void shouldCreateMissionWhenDroneAndOrderAreAvailable() {
        Drone drone = Drone.builder()
                .id("D1")
                .status(DroneStatus.IDLE)
                .batteryPercentage(100.0)
                .build();

        Order order = Order.builder()
                .id("O1")
                .status("PENDING")
                .build();

        List<Position> route = List.of(new Position(1.0, 1.0));

        MissionAssignmentResult result = policy.computeAssignment(drone, order, route);

        assertNotNull(result.mission());
        assertEquals("D1", result.mission().getDroneId());
        assertEquals("O1", result.mission().getOrderId());
        assertEquals(DroneStatus.MOVING, result.updatedDrone().getStatus());
        assertEquals("ASSIGNED", result.updatedOrder().getStatus());
        assertEquals(result.mission().getId(), result.updatedOrder().getAssignedMissionId());
    }

    @Test
    void shouldBeIdempotentIfAlreadyAssignedToSameDrone() {
        Drone drone = Drone.builder()
                .id("D1")
                .status(DroneStatus.MOVING)
                .currentMissionId("M1")
                .build();

        Order order = Order.builder()
                .id("O1")
                .status("ASSIGNED")
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
        Drone drone = Drone.builder()
                .id("D1")
                .status(DroneStatus.MOVING)
                .build();

        Order order = Order.builder()
                .id("O1")
                .status("PENDING")
                .build();

        assertThrows(BusinessRejectionException.class,
                () -> policy.computeAssignment(drone, order, List.of()));
    }

    @Test
    void shouldRejectIfOrderIsNotPending() {
        Drone drone = Drone.builder()
                .id("D1")
                .status(DroneStatus.IDLE)
                .batteryPercentage(100.0)
                .build();

        Order order = Order.builder()
                .id("O1")
                .status("ASSIGNED")
                .assignedDroneId("D2") // assigned to someone else
                .build();

        assertThrows(BusinessRejectionException.class,
                () -> policy.computeAssignment(drone, order, List.of()));
    }
}
