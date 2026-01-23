package com.dronefleet.statemanager.domain.port.in;

import com.dronefleet.statemanager.domain.model.Drone;
import java.util.List;

public interface GetFleetSnapshotUseCase {
    List<Drone> getFleetSnapshot();
}
