package com.dronefleet.statemanager.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.dronefleet.shared.models.Mission;
import com.dronefleet.statemanager.application.dto.MissionAssignmentDto;
import com.dronefleet.statemanager.domain.port.in.AssignMissionUseCase;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionCreationService implements AssignMissionUseCase {

    private final StateTransactionPort transactionPort;
    private final MissionAssignmentPolicy assignmentPolicy;

    @Override
    public Mission assignMission(MissionAssignmentDto dto) {
        log.info(
                "Requesting atomic mission assignment for orders {} to drone {}",
                dto.orderIds(),
                dto.droneId());

        return transactionPort.runMissionAssignmentTransaction(
                dto.droneId(),
                dto.orderIds(),
                context ->
                        assignmentPolicy.computeAssignment(context.drone(), context.orders(), dto));
    }
}
