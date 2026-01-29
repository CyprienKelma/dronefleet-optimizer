package com.dronefleet.statemanager.domain.port.in;

import java.util.List;

import com.dronefleet.shared.models.Drone;

public interface GetFleetSnapshotUseCase {
    List<Drone> getFleetSnapshot();
}
