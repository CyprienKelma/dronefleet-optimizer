package com.dronefleet.statemanager.infrastructure.adapter.in.rest;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dronefleet.shared.models.OptimizationSnapshot;
import com.dronefleet.statemanager.application.dto.OptimizationSnapshotDto;
import com.dronefleet.statemanager.domain.port.in.GetOptimizationSnapshotUseCase;

@RestController
@RequestMapping("/api/v1/optimizer")
@RequiredArgsConstructor
@Slf4j
public class OptimizerController {

    private final GetOptimizationSnapshotUseCase getSnapshotUseCase;

    @GetMapping("/snapshot")
    public ResponseEntity<OptimizationSnapshotDto> getSnapshot(
            @RequestParam(required = false) String sessionId) {
        String effectiveSessionId = sessionId != null ? sessionId : UUID.randomUUID().toString();

        log.info("Optimizer requesting snapshot with sessionId: {}", effectiveSessionId);

        OptimizationSnapshot snapshot = getSnapshotUseCase.getSnapshot(effectiveSessionId);

        OptimizationSnapshotDto dto = mapToDto(snapshot);

        return ResponseEntity.ok(dto);
    }

    private OptimizationSnapshotDto mapToDto(OptimizationSnapshot snapshot) {
        return new OptimizationSnapshotDto(
                snapshot.getSessionId(),
                Instant.ofEpochSecond(
                        snapshot.getTimestamp().getSeconds(), snapshot.getTimestamp().getNanos()),
                new OptimizationSnapshotDto.DepotDto(
                        snapshot.getDepot().getId(),
                        snapshot.getDepot().getName(),
                        new OptimizationSnapshotDto.PositionDto(
                                snapshot.getDepot().getPosition().getLat(),
                                snapshot.getDepot().getPosition().getLon())),
                snapshot.getDronesList().stream()
                        .map(
                                d ->
                                        new OptimizationSnapshotDto.DroneSnapshotDto(
                                                d.getId(),
                                                new OptimizationSnapshotDto.PositionDto(
                                                        d.getPosition().getLat(),
                                                        d.getPosition().getLon()),
                                                d.getBatteryPercentage(),
                                                d.getStatus().name(),
                                                d.getHomeDepotId(),
                                                d.getConsumptionPerKm(),
                                                d.getMaxFlightTimeMinutes()))
                        .collect(Collectors.toList()),
                snapshot.getOrdersList().stream()
                        .map(
                                o ->
                                        new OptimizationSnapshotDto.OrderSnapshotDto(
                                                o.getId(),
                                                new OptimizationSnapshotDto.PositionDto(
                                                        o.getDeliveryLocation().getLat(),
                                                        o.getDeliveryLocation().getLon()),
                                                o.getPriority().name(),
                                                o.getProductType(),
                                                Instant.ofEpochSecond(
                                                        o.getCreatedAt().getSeconds(),
                                                        o.getCreatedAt().getNanos())))
                        .collect(Collectors.toList()),
                snapshot.getWarehousesList().stream()
                        .map(
                                w ->
                                        new OptimizationSnapshotDto.WarehouseSnapshotDto(
                                                w.getId(),
                                                w.getName(),
                                                new OptimizationSnapshotDto.PositionDto(
                                                        w.getPosition().getLat(),
                                                        w.getPosition().getLon()),
                                                w.getAuthorizedProductTypesList(),
                                                w.getIsColdStorageCapable()))
                        .collect(Collectors.toList()));
    }
}
