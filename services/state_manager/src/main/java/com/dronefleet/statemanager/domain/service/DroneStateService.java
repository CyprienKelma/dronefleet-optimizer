package com.dronefleet.statemanager.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dronefleet.shared.models.DroneTelemetry;
import com.dronefleet.statemanager.domain.port.in.UpdateDroneStateUseCase;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort;

@Slf4j
@Service
public class DroneStateService implements UpdateDroneStateUseCase {

    private final StateTransactionPort transactionPort;

    @Autowired
    public DroneStateService(StateTransactionPort transactionPort) {
        this.transactionPort = transactionPort;
    }

    /**
     * Handle the new state of a drone from telemetry data.
     *
     * @param telemetry The telemetry data to update the drone state from.
     */
    @Override
    public void handleTelemetry(DroneTelemetry telemetry) {
        log.info("Requesting atomic telemetry update for drone {}", telemetry.droneId());
        transactionPort.runTelemetryUpdateTransaction(telemetry);
    }
}
