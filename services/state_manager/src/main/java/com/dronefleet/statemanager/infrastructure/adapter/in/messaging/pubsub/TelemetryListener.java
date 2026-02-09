package com.dronefleet.statemanager.infrastructure.adapter.in.messaging.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

import com.dronefleet.shared.models.DroneTelemetry;
import com.dronefleet.shared.models.Position;
import com.dronefleet.statemanager.application.dto.TelemetryEventDto;
import com.dronefleet.statemanager.domain.exception.BusinessRejectionException;
import com.dronefleet.statemanager.domain.port.in.UpdateDroneStateUseCase;
import com.dronefleet.statemanager.domain.service.DronePolicy;

/** Inbound adapter that listens to Pub/Sub messages and routes them to the domain. */
@Slf4j
@Component
public class TelemetryListener {

    private final UpdateDroneStateUseCase updateDroneStateUseCase;
    private final ObjectMapper objectMapper;
    private final DronePolicy dronePolicy;

    public TelemetryListener(
            UpdateDroneStateUseCase updateDroneStateUseCase,
            ObjectMapper objectMapper,
            DronePolicy dronePolicy) {
        this.updateDroneStateUseCase = updateDroneStateUseCase;
        this.objectMapper = objectMapper;
        this.dronePolicy = dronePolicy;
    }

    @ServiceActivator(inputChannel = "telemetryInputChannel")
    public void handleTelemetryMessage(String payload) {
        try {
            log.debug("Received telemetry payload: {}", payload);
            TelemetryEventDto dto = objectMapper.readValue(payload, TelemetryEventDto.class);

            // mapping DTO -> domain model (Immutable Builder)
            DroneTelemetry droneDomainModel =
                    DroneTelemetry.newBuilder()
                            .setDroneId(dto.droneId() != null ? dto.droneId() : "UNKNOWN")
                            .setTimestamp(
                                    dto.timestamp() != null
                                            ? com.google.protobuf.Timestamp.newBuilder()
                                                    .setSeconds(dto.timestamp().toEpochSecond())
                                                    .setNanos(dto.timestamp().getNano())
                                                    .build()
                                            : com.google.protobuf.Timestamp.newBuilder()
                                                    .setSeconds(
                                                            java.time.Instant.now()
                                                                    .getEpochSecond())
                                                    .build())
                            .setPosition(
                                    Position.newBuilder()
                                            .setLat(
                                                    dto.position() != null
                                                            ? dto.position().lat()
                                                            : 0.0)
                                            .setLon(
                                                    dto.position() != null
                                                            ? dto.position().lon()
                                                            : 0.0)
                                            .build())
                            .setBatteryPercentage(dto.batteryPercentage())
                            .setSpeedKmh(dto.speedKmh())
                            .setStatus(dronePolicy.parseStatus(dto.status()))
                            .setCurrentMissionId(
                                    dto.currentMissionId() != null ? dto.currentMissionId() : "")
                            .build();

            updateDroneStateUseCase.handleTelemetry(droneDomainModel);
        } catch (BusinessRejectionException e) {
            log.warn("Telemetry update rejected: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing telemetry message: {}. Retrying.", e.getMessage());
            throw new RuntimeException("Error processing telemetry message", e);
        }
    }
}
