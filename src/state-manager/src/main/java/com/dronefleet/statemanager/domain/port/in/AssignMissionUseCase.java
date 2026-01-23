package com.dronefleet.statemanager.domain.port.in;

import com.dronefleet.statemanager.domain.model.Position;
import java.util.List;

public interface AssignMissionUseCase {
    void assignMission(String droneId, String orderId, List<Position> route);
}
