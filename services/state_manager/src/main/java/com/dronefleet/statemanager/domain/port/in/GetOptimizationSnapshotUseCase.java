package com.dronefleet.statemanager.domain.port.in;

import com.dronefleet.shared.models.OptimizationSnapshot;

public interface GetOptimizationSnapshotUseCase {
    OptimizationSnapshot acquireSnapshot(String solvingSessionId);
}
