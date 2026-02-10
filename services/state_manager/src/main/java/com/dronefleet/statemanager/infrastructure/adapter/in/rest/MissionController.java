package com.dronefleet.statemanager.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dronefleet.shared.models.Mission;
import com.dronefleet.shared.models.Waypoint;
import com.dronefleet.statemanager.application.dto.MissionDto;
import com.dronefleet.statemanager.domain.port.in.GetMissionsUseCase;

/**
 * Inbound REST adapter for querying missions produced by the optimizer.
 *
 * <p>Follows the same hexagonal pattern as {@link OptimizerController}: inject the use-case port,
 * map domain objects to DTOs, return ResponseEntity.
 */
@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
@Slf4j
public class MissionController {

    private final GetMissionsUseCase getMissionsUseCase;

    /** List all missions. */
    @GetMapping
    public ResponseEntity<List<MissionDto>> getAllMissions() {
        log.info("GET /api/v1/missions - fetching all missions");
        List<MissionDto> missions =
                getMissionsUseCase.getAllMissions().stream()
                        .map(this::mapToDto)
                        .collect(Collectors.toList());
        return ResponseEntity.ok(missions);
    }

    /** Get a single mission by its identifier. */
    @GetMapping("/{id}")
    public ResponseEntity<MissionDto> getMissionById(@PathVariable String id) {
        log.info("GET /api/v1/missions/{} - fetching mission", id);
        return getMissionsUseCase
                .getMissionById(id)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------------
    // Domain-to-DTO mapping
    // ---------------------------------------------------------------

    private MissionDto mapToDto(Mission mission) {
        Instant startTime = null;
        if (mission.hasStartTime()) {
            startTime =
                    Instant.ofEpochSecond(
                            mission.getStartTime().getSeconds(), mission.getStartTime().getNanos());
        }

        return new MissionDto(
                mission.getId(),
                mission.getDroneId(),
                mission.getOrderIdsList(),
                mission.getStatus(),
                mission.getRouteList().stream()
                        .map(this::mapWaypointToDto)
                        .collect(Collectors.toList()),
                mission.getEstimatedBatteryConsumption(),
                mission.getEstimatedDurationMinutes(),
                startTime);
    }

    private MissionDto.WaypointDto mapWaypointToDto(Waypoint waypoint) {
        MissionDto.PositionDto position = null;
        if (waypoint.hasPosition()) {
            position =
                    new MissionDto.PositionDto(
                            waypoint.getPosition().getLat(), waypoint.getPosition().getLon());
        }

        return new MissionDto.WaypointDto(
                waypoint.getType().name(),
                position,
                waypoint.getRelatedOrderId(),
                waypoint.getRelatedWarehouseId());
    }
}
