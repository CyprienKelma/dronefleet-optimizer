package com.dronefleet.statemanager.domain.service;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.dronefleet.shared.models.Mission;
import com.dronefleet.shared.models.Position;
import com.dronefleet.statemanager.domain.port.in.AssignMissionUseCase;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort;

@Slf4j
@Service
public class MissionCreationService implements AssignMissionUseCase {

    private final StateTransactionPort transactionPort;
    private final MissionAssignmentPolicy assignmentPolicy;

    public MissionCreationService(
            StateTransactionPort transactionPort, MissionAssignmentPolicy assignmentPolicy) {
        this.transactionPort = transactionPort;
        this.assignmentPolicy = assignmentPolicy;
    }

    @Override
    public Mission assignMission(String droneId, String orderId, List<Position> route) {
        log.info("Requesting atomic mission assignment for order {} to drone {}", orderId, droneId);

        return transactionPort.runMissionAssignmentTransaction(
                droneId,
                orderId,
                context ->
                        assignmentPolicy.computeAssignment(
                                context.drone(), context.order(), route));
    }
}
