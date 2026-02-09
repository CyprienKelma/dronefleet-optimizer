package com.dronefleet.statemanager.domain.port.in;

import com.dronefleet.shared.models.Mission;
import com.dronefleet.statemanager.application.dto.MissionAssignmentDto;

public interface AssignMissionUseCase {
    /**
     * Assign a mission to a drone based on the optimizer's decision.
     *
     * @param dto the mission assignment details
     * @return the created mission
     */
    Mission assignMission(MissionAssignmentDto dto);
}
