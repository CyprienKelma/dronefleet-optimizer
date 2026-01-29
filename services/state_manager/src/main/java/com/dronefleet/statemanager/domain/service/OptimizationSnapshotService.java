package com.dronefleet.statemanager.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.dronefleet.shared.models.OptimizationSnapshot;
import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.port.in.GetOptimizationSnapshotUseCase;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizationSnapshotService implements GetOptimizationSnapshotUseCase {

    private final StateTransactionPort stateTransactionPort;
    private final AppProperties appProperties;

    @Override
    public OptimizationSnapshot acquireSnapshot(String solvingSessionId) {
        log.info("Acquiring optimization snapshot for session: {}", solvingSessionId);
        return stateTransactionPort.runSnapshotAcquisitionTransaction(
                solvingSessionId, appProperties.getMinBatteryForOptimization());
    }
}
