package com.dronefleet.statemanager.domain.port.in;

import java.util.List;

import com.dronefleet.shared.models.Mission;
import com.dronefleet.shared.models.Position;

public interface AssignMissionUseCase {
    /**
     * Assign a mission to a drone.
     *
     * @param droneId the id of the drone
     * @param orderId the id of the order
     * @param route the route of the mission
     * @return the created mission
     */
    Mission assignMission(String droneId, String orderId, List<Position> route);
}
