package com.dronefleet.statemanager.domain.port.in;

import com.dronefleet.statemanager.domain.model.OptimizationSnapshot;

public interface GetOptimizationSnapshotUseCase {
    OptimizationSnapshot acquireSnapshot(String solvingSessionId);
}
