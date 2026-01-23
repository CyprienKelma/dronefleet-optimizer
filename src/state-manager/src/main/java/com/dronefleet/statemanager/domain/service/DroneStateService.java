package com.dronefleet.statemanager.domain.service;

import com.dronefleet.statemanager.domain.model.Drone;
import com.dronefleet.statemanager.domain.model.DroneTelemetry;
import com.dronefleet.statemanager.domain.port.in.UpdateDroneStateUseCase;
import com.dronefleet.statemanager.domain.port.out.DroneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class DroneStateService implements UpdateDroneStateUseCase {

    private final DroneRepository droneRepository;

    /**
     * Update the state of a drone from telemetry data.
     *
     * @param telemetry The telemetry data to update the drone state from.
     */
    @Override
    public void handleTelemetry(DroneTelemetry telemetry) {
        log.info("Processing telemetry for drone {}: Status={}, Battery={}%, Pos=({}, {})",
                telemetry.droneId(), telemetry.status(), telemetry.batteryPercentage(),
                telemetry.position().lat(), telemetry.position().lon());

        Drone drone = droneRepository.findById(telemetry.droneId())
                .orElse(Drone.builder()
                        .id(telemetry.droneId())
                        .build());

        drone.updateTelemetry(
                telemetry.position(),
                telemetry.batteryPercentage(),
                telemetry.speedKmh(),
                telemetry.status(),
                telemetry.currentMissionId()
        );

        droneRepository.save(drone);
    }
}
