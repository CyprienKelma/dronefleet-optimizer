package com.dronefleet.statemanager.infrastructure.adapter.in.messaging.pubsub;

import com.dronefleet.statemanager.application.dto.TelemetryEventDto;
import com.dronefleet.statemanager.domain.model.DroneStatus;
import com.dronefleet.statemanager.domain.model.DroneTelemetry;
import com.dronefleet.statemanager.domain.model.Position;
import com.dronefleet.statemanager.domain.port.in.UpdateDroneStateUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;
import com.dronefleet.statemanager.domain.exception.BusinessRejectionException;

/**
 * Inbound adapter that listens to Pub/Sub messages and routes them to the domain.
 */
@Slf4j
@Component
public class TelemetryListener {

    private final UpdateDroneStateUseCase updateDroneStateUseCase;
    private final ObjectMapper objectMapper;

    public TelemetryListener(UpdateDroneStateUseCase updateDroneStateUseCase, ObjectMapper objectMapper) {
        this.updateDroneStateUseCase = updateDroneStateUseCase;
        this.objectMapper = objectMapper;
    }

    @ServiceActivator(inputChannel = "telemetryInputChannel")
    public void handleTelemetryMessage(String payload) {
        try {
            log.debug("Received telemetry payload: {}", payload);
            TelemetryEventDto dto = objectMapper.readValue(payload, TelemetryEventDto.class);

            // mapping DTO -> domain model
            DroneTelemetry DroneDomainModel = new DroneTelemetry(
                dto.droneId(),
                dto.timestamp(),
                new Position(dto.position().lat(), dto.position().lon()),
                dto.batteryPercentage(),
                dto.speedKmh(),
                DroneStatus.parseStatus(dto.status()),
                dto.currentMissionId()
            );

            updateDroneStateUseCase.handleTelemetry(DroneDomainModel);
        } catch (BusinessRejectionException e) {
            log.warn("Telemetry update rejected: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing telemetry message: {}. Retrying.", e.getMessage());
            throw new RuntimeException("Error processing telemetry message", e);
        }
    }
}
