package com.dronefleet.statemanager.domain.port.in;

import com.dronefleet.statemanager.domain.model.Mission;
import com.dronefleet.statemanager.domain.model.Position;
import java.util.List;

public interface AssignMissionUseCase {
    /**
     * Assign a mission to a drone.
     * @param droneId the id of the drone
     * @param orderId the id of the order
     * @param route the route of the mission
     * @return the created mission
     */
    Mission assignMission(String droneId, String orderId, List<Position> route);
}
