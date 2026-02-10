package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.dronefleet.shared.models.Drone;
import com.dronefleet.shared.models.DroneTelemetry;
import com.dronefleet.shared.models.Mission;
import com.dronefleet.shared.models.OptimizationSnapshot;
import com.dronefleet.shared.models.Order;
import com.dronefleet.shared.models.OrderStatus;
import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort;
import com.dronefleet.statemanager.domain.port.out.WarehouseRepository;
import com.dronefleet.statemanager.domain.service.DronePolicy;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirestoreStateTransactionAdapter implements StateTransactionPort {

    private final Firestore firestore;
    private final AppProperties appProperties;
    private final FirestoreMapper mapper;
    private final WarehouseRepository warehouseRepository;
    private final DronePolicy dronePolicy;

    @Override
    public Mission runMissionAssignmentTransaction(
            String droneId,
            List<String> orderIds,
            Function<DroneOrdersContext, MissionAssignmentResult> assignmentLogic) {

        ApiFuture<Mission> result =
                firestore.runTransaction(
                        transaction -> {
                            DocumentReference droneRef =
                                    firestore
                                            .collection(appProperties.getDronesCollection())
                                            .document(droneId);

                            List<DocumentReference> orderRefs =
                                    orderIds.stream()
                                            .map(
                                                    id ->
                                                            firestore
                                                                    .collection(
                                                                            appProperties
                                                                                    .getOrdersCollection())
                                                                    .document(id))
                                            .collect(Collectors.toList());

                            // 1. Reads (must come before writes)
                            DocumentSnapshot droneSnap = transaction.get(droneRef).get();

                            List<DocumentSnapshot> orderSnaps = new ArrayList<>();
                            for (DocumentReference ref : orderRefs) {
                                orderSnaps.add(transaction.get(ref).get());
                            }

                            if (!droneSnap.exists()) {
                                throw new RuntimeException("Drone not found: " + droneId);
                            }

                            for (int i = 0; i < orderIds.size(); i++) {
                                if (!orderSnaps.get(i).exists()) {
                                    throw new RuntimeException(
                                            "Order not found: " + orderIds.get(i));
                                }
                            }

                            Drone drone = mapper.mapToDrone(droneSnap);
                            List<Order> orders =
                                    orderSnaps.stream()
                                            .map(mapper::mapToOrder)
                                            .collect(Collectors.toList());

                            // 2. Business Logic
                            MissionAssignmentResult assignmentResult =
                                    assignmentLogic.apply(new DroneOrdersContext(drone, orders));

                            // 3. Writes
                            if (assignmentResult.mission() != null) {
                                DocumentReference missionRef =
                                        firestore
                                                .collection(appProperties.getMissionsCollection())
                                                .document(assignmentResult.mission().getId());
                                transaction.set(
                                        missionRef,
                                        mapper.mapFromMission(assignmentResult.mission()));
                            }

                            transaction.set(
                                    droneRef, mapper.mapFromDrone(assignmentResult.updatedDrone()));

                            for (int i = 0; i < orderRefs.size(); i++) {
                                transaction.set(
                                        orderRefs.get(i),
                                        mapper.mapFromOrder(
                                                assignmentResult.updatedOrders().get(i)));
                            }

                            return assignmentResult.mission();
                        });

        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Mission assignment transaction failed", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException("Transaction failed", e);
        }
    }

    @Override
    public void runTelemetryUpdateTransaction(DroneTelemetry telemetry) {
        ApiFuture<Void> result =
                firestore.runTransaction(
                        transaction -> {
                            DocumentReference droneRef =
                                    firestore
                                            .collection(appProperties.getDronesCollection())
                                            .document(telemetry.getDroneId());
                            DocumentSnapshot droneSnap = transaction.get(droneRef).get();

                            Drone drone;
                            if (droneSnap.exists()) {
                                drone = mapper.mapToDrone(droneSnap);

                                // Ordering check: if incoming telemetry is older than last update,
                                // skip
                                if (drone.hasLastUpdate()) {
                                    Instant currentLastUpdate =
                                            Instant.ofEpochSecond(
                                                    drone.getLastUpdate().getSeconds(),
                                                    drone.getLastUpdate().getNanos());
                                    Instant incomingTimestamp =
                                            Instant.ofEpochSecond(
                                                    telemetry.getTimestamp().getSeconds(),
                                                    telemetry.getTimestamp().getNanos());

                                    if (incomingTimestamp.isBefore(currentLastUpdate)) {
                                        log.info(
                                                "Stale telemetry for drone {}. Current: {},"
                                                        + " Incoming: {}. Skipping.",
                                                drone.getId(),
                                                currentLastUpdate,
                                                incomingTimestamp);
                                        return null;
                                    }
                                }
                            } else {
                                drone = Drone.newBuilder().setId(telemetry.getDroneId()).build();
                            }

                            Drone updatedDrone =
                                    dronePolicy.applyTelemetryUpdate(
                                            drone,
                                            telemetry.getPosition(),
                                            telemetry.getBatteryPercentage(),
                                            telemetry.getSpeedKmh(),
                                            telemetry.getStatus(),
                                            telemetry.getCurrentMissionId(),
                                            Instant.ofEpochSecond(
                                                    telemetry.getTimestamp().getSeconds(),
                                                    telemetry.getTimestamp().getNanos()));

                            transaction.set(droneRef, mapper.mapFromDrone(updatedDrone));
                            return null;
                        });

        try {
            result.get(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error(
                    "Telemetry update transaction failed for drone {}", telemetry.getDroneId(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void runOrderIngestionTransaction(Order order) {
        ApiFuture<Void> result =
                firestore.runTransaction(
                        transaction -> {
                            DocumentReference orderRef =
                                    firestore
                                            .collection(appProperties.getOrdersCollection())
                                            .document(order.getId());
                            DocumentSnapshot orderSnap = transaction.get(orderRef).get();

                            if (orderSnap.exists()) {
                                Order existing = mapper.mapToOrder(orderSnap);
                                // If order already exists and is not PENDING, we don't want to
                                // reset
                                // it
                                if (existing.getStatus() != OrderStatus.ORDER_STATUS_PENDING
                                        && existing.getStatus()
                                                != OrderStatus.ORDER_STATUS_UNSPECIFIED) {
                                    log.info(
                                            "Order {} already exists with status {}. Skipping"
                                                    + " ingestion.",
                                            order.getId(),
                                            existing.getStatus());
                                    return null;
                                }
                            }

                            Order orderToSave = order;
                            if (order.getStatus() == OrderStatus.ORDER_STATUS_UNSPECIFIED) {
                                orderToSave =
                                        order.toBuilder()
                                                .setStatus(OrderStatus.ORDER_STATUS_PENDING)
                                                .build();
                            }

                            transaction.set(orderRef, mapper.mapFromOrder(orderToSave));
                            return null;
                        });

        try {
            result.get(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Order ingestion transaction failed for order {}", order.getId(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public OptimizationSnapshot runSnapshotAcquisitionTransaction(
            String sessionId, int minBatteryPercent) {
        return null;
    }
}
