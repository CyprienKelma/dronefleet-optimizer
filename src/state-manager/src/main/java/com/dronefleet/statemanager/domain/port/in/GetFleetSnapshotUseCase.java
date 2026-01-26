package com.dronefleet.statemanager.domain.port.in;

import java.util.List;

import com.dronefleet.statemanager.domain.model.Drone;

public interface GetFleetSnapshotUseCase {
    List<Drone> getFleetSnapshot();
}
