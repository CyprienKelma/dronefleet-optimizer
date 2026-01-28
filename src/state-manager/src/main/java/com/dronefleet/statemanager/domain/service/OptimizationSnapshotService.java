package com.dronefleet.statemanager.domain.service;

import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.model.OptimizationSnapshot;
import com.dronefleet.statemanager.domain.port.in.GetOptimizationSnapshotUseCase;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
                solvingSessionId,
                appProperties.getMinBatteryForOptimization()
        );
    }
}
