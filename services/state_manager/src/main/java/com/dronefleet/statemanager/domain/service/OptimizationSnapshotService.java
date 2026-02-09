package com.dronefleet.statemanager.domain.service;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.dronefleet.shared.models.Depot;
import com.dronefleet.shared.models.Drone;
import com.dronefleet.shared.models.OptimizationSnapshot;
import com.dronefleet.shared.models.Order;
import com.dronefleet.shared.models.Warehouse;
import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.exception.BusinessRejectionException;
import com.dronefleet.statemanager.domain.port.in.GetOptimizationSnapshotUseCase;
import com.dronefleet.statemanager.domain.port.out.DepotRepository;
import com.dronefleet.statemanager.domain.port.out.DroneRepository;
import com.dronefleet.statemanager.domain.port.out.OrderRepository;
import com.dronefleet.statemanager.domain.port.out.WarehouseRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizationSnapshotService implements GetOptimizationSnapshotUseCase {

    private final DroneRepository droneRepository;
    private final OrderRepository orderRepository;
    private final WarehouseRepository warehouseRepository;
    private final DepotRepository depotRepository;
    private final AppProperties appProperties;

    @Override
    public OptimizationSnapshot getSnapshot(String sessionId) {
        log.info("Creating optimization snapshot with sessionId: {}", sessionId);

        List<Drone> idleDrones =
                droneRepository.findAvailableForOptimization(
                        appProperties.getMinBatteryForOptimization());

        List<Order> pendingOrders = orderRepository.findPending();

        Depot depot =
                depotRepository
                        .findMainDepot()
                        .orElseThrow(
                                () -> new BusinessRejectionException("No main depot configured"));

        List<Warehouse> warehouses = warehouseRepository.findAll();

        Instant now = Instant.now();
        return OptimizationSnapshot.newBuilder()
                .setSessionId(sessionId)
                .setTimestamp(
                        com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(now.getEpochSecond())
                                .setNanos(now.getNano())
                                .build())
                .addAllDrones(idleDrones)
                .setDepot(depot)
                .addAllWarehouses(warehouses)
                .addAllOrders(pendingOrders)
                .build();
    }
}
