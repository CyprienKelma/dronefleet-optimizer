package com.dronefleet.statemanager.domain.port.in;

import com.dronefleet.statemanager.domain.model.DroneTelemetry;

/** Use case interface for updating the state of a drone from telemetry data. */
public interface UpdateDroneStateUseCase {
    /**
     * Update the state of a drone from telemetry data.
     *
     * @param telemetry The telemetry data to update the drone state from.
     */
    void handleTelemetry(DroneTelemetry telemetry);
}
