package com.dronefleet.statemanager.infrastructure.adapter.in.messaging.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

import com.dronefleet.statemanager.application.dto.MissionAssignmentDto;
import com.dronefleet.statemanager.domain.port.in.AssignMissionUseCase;

/**
 * Inbound adapter that listens to Pub/Sub messages for optimizer decisions and routes them to the
 * domain.
 */
@Slf4j
@Component
public class DecisionListener {

    private final AssignMissionUseCase assignMissionUseCase;
    private final ObjectMapper objectMapper;

    public DecisionListener(AssignMissionUseCase assignMissionUseCase, ObjectMapper objectMapper) {
        this.assignMissionUseCase = assignMissionUseCase;
        this.objectMapper = objectMapper;
    }

    @ServiceActivator(inputChannel = "decisionsInputChannel")
    public void handleDecisionMessage(String payload) {
        try {
            log.info("Received optimizer decision payload: {}", payload);
            MissionAssignmentDto dto = objectMapper.readValue(payload, MissionAssignmentDto.class);

            assignMissionUseCase.assignMission(dto);

            log.info(
                    "Successfully processed decision for drone {} and orders {}",
                    dto.droneId(),
                    dto.orderIds());
        } catch (com.dronefleet.statemanager.domain.exception.BusinessRejectionException e) {
            log.warn("Optimizer decision rejected by business rules: {}", e.getMessage());
            // Business rejection should not be retried as it's a "permanent" failure of this
            // decision
        } catch (Exception e) {
            log.error(
                    "Transient error processing optimizer decision message: {}. Will be retried.",
                    e.getMessage(),
                    e);
            throw new RuntimeException(
                    "Error processing message",
                    e); // Throwing re-triggers retry in Spring Cloud GCP
        }
    }
}
