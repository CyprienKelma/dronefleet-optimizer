package com.dronefleet.statemanager.domain.service;

import com.dronefleet.statemanager.domain.model.Drone;
import com.dronefleet.statemanager.domain.model.DroneStatus;
import com.dronefleet.statemanager.domain.model.Mission;
import com.dronefleet.statemanager.domain.model.Order;
import com.dronefleet.statemanager.domain.model.Position;
import com.dronefleet.statemanager.domain.port.in.AssignMissionUseCase;
import com.dronefleet.statemanager.domain.port.out.DroneRepository;
import com.dronefleet.statemanager.domain.port.out.MissionRepository;
import com.dronefleet.statemanager.domain.port.out.OrderRepository;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class MissionCreationService implements AssignMissionUseCase {

    private final StateTransactionPort transactionPort;
    private final MissionAssignmentPolicy assignmentPolicy;

    public MissionCreationService(StateTransactionPort transactionPort,
                                  MissionAssignmentPolicy assignmentPolicy) {
        this.transactionPort = transactionPort;
        this.assignmentPolicy = assignmentPolicy;
    }

    @Override
    public Mission assignMission(String droneId, String orderId, List<Position> route) {
        log.info("Requesting atomic mission assignment for order {} to drone {}", orderId, droneId);

        return transactionPort.runMissionAssignmentTransaction(droneId, orderId, context ->
            assignmentPolicy.computeAssignment(context.drone(), context.order(), route)
        );
    }
}
