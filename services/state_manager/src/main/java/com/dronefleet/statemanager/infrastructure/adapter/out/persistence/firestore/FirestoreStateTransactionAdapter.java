package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.dronefleet.shared.models.Drone;
import com.dronefleet.shared.models.DroneStatus;
import com.dronefleet.shared.models.DroneTelemetry;
import com.dronefleet.shared.models.Mission;
import com.dronefleet.shared.models.OptimizationSnapshot;
import com.dronefleet.shared.models.Order;
import com.dronefleet.shared.models.OrderStatus;
import com.dronefleet.shared.models.Warehouse;
import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort;
import com.dronefleet.statemanager.domain.port.out.WarehouseRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirestoreStateTransactionAdapter implements StateTransactionPort {

    private final Firestore firestore;
    private final AppProperties appProperties;
    private final FirestoreMapper mapper;
    private final WarehouseRepository warehouseRepository;

    @Override
    public Mission runMissionAssignmentTransaction(
            String droneId,
            String orderId,
            Function<DroneOrderContext, MissionAssignmentResult> assignmentLogic) {

        ApiFuture<Mission> result =
                firestore.runTransaction(
                        transaction -> {
                            DocumentReference droneRef =
                                    firestore
                                            .collection(appProperties.getDronesCollection())
                                            .document(droneId);
                            DocumentReference orderRef =
                                    firestore
                                            .collection(appProperties.getOrdersCollection())
                                            .document(orderId);

                            // 1. Reads (must come before writes)
                            DocumentSnapshot droneSnap = transaction.get(droneRef).get();
                            DocumentSnapshot orderSnap = transaction.get(orderRef).get();

                            if (!droneSnap.exists()) {
                                throw new RuntimeException("Drone not found: " + droneId);
                            }
                            if (!orderSnap.exists()) {
                                throw new RuntimeException("Order not found: " + orderId);
                            }

                            Drone drone = mapper.mapToDrone(droneSnap);
                            Order order = mapper.mapToOrder(orderSnap);

                            // 2. Business Logic
                            MissionAssignmentResult assignmentResult =
                                    assignmentLogic.apply(new DroneOrderContext(drone, order));

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
                            transaction.set(
                                    orderRef, mapper.mapFromOrder(assignmentResult.updatedOrder()));

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
        firestore.runTransaction(
                transaction -> {
                    DocumentReference droneRef =
                            firestore
                                    .collection(appProperties.getDronesCollection())
                                    .document(telemetry.droneId());
                    DocumentSnapshot droneSnap = transaction.get(droneRef).get();

                    Drone drone;
                    if (droneSnap.exists()) {
                        drone = mapper.mapToDrone(droneSnap);

                        // Ordering check: if incoming telemetry is older than last update, skip
                        if (drone.getLastUpdate() != null
                                && telemetry
                                        .timestamp()
                                        .toInstant()
                                        .isBefore(drone.getLastUpdate())) {
                            log.info(
                                    "Stale telemetry for drone {}. Current: {}, Incoming: {}."
                                            + " Skipping.",
                                    drone.getId(),
                                    drone.getLastUpdate(),
                                    telemetry.timestamp());
                            return null;
                        }
                    } else {
                        drone = Drone.builder().id(telemetry.droneId()).build();
                    }

                    drone.updateTelemetry(
                            telemetry.position(),
                            telemetry.batteryPercentage(),
                            telemetry.speedKmh(),
                            telemetry.status(),
                            telemetry.currentMissionId(),
                            telemetry.timestamp().toInstant());

                    transaction.set(droneRef, mapper.mapFromDrone(drone));
                    return null;
                });
    }

    @Override
    public void runOrderIngestionTransaction(Order order) {
        firestore.runTransaction(
                transaction -> {
                    DocumentReference orderRef =
                            firestore
                                    .collection(appProperties.getOrdersCollection())
                                    .document(order.getId());
                    DocumentSnapshot orderSnap = transaction.get(orderRef).get();

                    if (orderSnap.exists()) {
                        Order existing = mapper.mapToOrder(orderSnap);
                        // If order already exists and is not PENDING, we don't want to reset it
                        if (!"PENDING".equals(existing.getStatus())
                                && existing.getStatus() != null) {
                            log.info(
                                    "Order {} already exists with status {}. Skipping ingestion.",
                                    order.getId(),
                                    existing.getStatus());
                            return null;
                        }
                    }

                    if (order.getStatus() == null) {
                        order.setStatus(OrderStatus.PENDING);
                    }

                    transaction.set(orderRef, mapper.mapFromOrder(order));
                    return null;
                });
    }

    @Override
    public OptimizationSnapshot runSnapshotAcquisitionTransaction(
            String sessionId, int minBatteryPercent) {
        // 1. Reads that don't need to be in the transaction or can be done before
        List<Warehouse> warehouses = warehouseRepository.findAll();

        ApiFuture<OptimizationSnapshot> result =
                firestore.runTransaction(
                        transaction -> {
                            List<Drone> availableDrones = new ArrayList<>();
                            List<Order> pendingOrders = new ArrayList<>();

                            // 1. Queries (READS)
                            // We get the snapshots first to know which documents to lock/read in
                            // the transaction
                            List<QueryDocumentSnapshot> droneDocs =
                                    firestore
                                            .collection(appProperties.getDronesCollection())
                                            .whereEqualTo("status", DroneStatus.IDLE.name())
                                            .whereGreaterThanOrEqualTo(
                                                    "batteryPercentage", (double) minBatteryPercent)
                                            .get()
                                            .get()
                                            .getDocuments();

                            List<QueryDocumentSnapshot> orderDocs =
                                    firestore
                                            .collection(appProperties.getOrdersCollection())
                                            .whereEqualTo("status", OrderStatus.PENDING.name())
                                            .get()
                                            .get()
                                            .getDocuments();

                            // 2. Transactional Reads
                            // Collect all references to read them atomically
                            List<DocumentReference> droneRefs =
                                    droneDocs.stream()
                                            .map(QueryDocumentSnapshot::getReference)
                                            .collect(Collectors.toList());
                            List<DocumentReference> orderRefs =
                                    orderDocs.stream()
                                            .map(QueryDocumentSnapshot::getReference)
                                            .collect(Collectors.toList());

                            List<DocumentReference> allRefs = new ArrayList<>();
                            allRefs.addAll(droneRefs);
                            allRefs.addAll(orderRefs);

                            if (allRefs.isEmpty()) {
                                return OptimizationSnapshot.builder()
                                        .drones(availableDrones)
                                        .orders(pendingOrders)
                                        .warehouses(warehouses)
                                        .sessionId(sessionId)
                                        .timestamp(Instant.now())
                                        .build();
                            }

                            // Read all documents in one go (ALL READS)
                            List<DocumentSnapshot> allSnapshots =
                                    transaction
                                            .getAll(
                                                    Objects.requireNonNull(
                                                            allRefs.toArray(
                                                                    new DocumentReference[0])))
                                            .get();

                            // 3. Business Logic & Writes
                            // Now that all reads are done, we can start the writes
                            for (DocumentSnapshot snap : allSnapshots) {
                                if (!snap.exists()) {
                                    continue;
                                }

                                String collectionName = snap.getReference().getParent().getId();

                                if (collectionName.equals(appProperties.getDronesCollection())) {
                                    Drone drone = mapper.mapToDrone(snap);
                                    if (drone.getStatus() == DroneStatus.IDLE
                                            && drone.getBatteryPercentage() >= minBatteryPercent) {
                                        drone.setStatus(DroneStatus.RESERVED);
                                        drone.setSolvingSessionId(sessionId);
                                        transaction.set(
                                                snap.getReference(), mapper.mapFromDrone(drone));
                                        availableDrones.add(drone);
                                    }
                                } else if (collectionName.equals(
                                        appProperties.getOrdersCollection())) {
                                    Order order = mapper.mapToOrder(snap);
                                    if (order.getStatus() == OrderStatus.PENDING) {
                                        order.setStatus(OrderStatus.SOLVING);
                                        order.setSolvingSessionId(sessionId);
                                        transaction.set(
                                                snap.getReference(), mapper.mapFromOrder(order));
                                        pendingOrders.add(order);
                                    }
                                }
                            }

                            return OptimizationSnapshot.builder()
                                    .drones(availableDrones)
                                    .orders(pendingOrders)
                                    .warehouses(warehouses)
                                    .sessionId(sessionId)
                                    .timestamp(Instant.now())
                                    .build();
                        });

        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Snapshot acquisition transaction failed", e);
            throw new RuntimeException("Transaction failed", e);
        }
    }
}
