package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.model.Drone;
import com.dronefleet.statemanager.domain.model.DroneTelemetry;
import com.dronefleet.statemanager.domain.model.Mission;
import com.dronefleet.statemanager.domain.model.Order;
import com.dronefleet.statemanager.domain.port.out.StateTransactionPort;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirestoreStateTransactionAdapter implements StateTransactionPort {

    private final Firestore firestore;
    private final AppProperties appProperties;
    private final FirestoreMapper mapper;

    @Override
    public Mission runMissionAssignmentTransaction(
            String droneId,
            String orderId,
            Function<DroneOrderContext, MissionAssignmentResult> assignmentLogic) {

        ApiFuture<Mission> result = firestore.runTransaction(transaction -> {
            DocumentReference droneRef = firestore.collection(appProperties.getDronesCollection()).document(droneId);
            DocumentReference orderRef = firestore.collection(appProperties.getOrdersCollection()).document(orderId);

            // 1. Reads (must come before writes)
            DocumentSnapshot droneSnap = transaction.get(droneRef).get();
            DocumentSnapshot orderSnap = transaction.get(orderRef).get();

            if (!droneSnap.exists()) throw new RuntimeException("Drone not found: " + droneId);
            if (!orderSnap.exists()) throw new RuntimeException("Order not found: " + orderId);

            Drone drone = mapper.mapToDrone(droneSnap);
            Order order = mapper.mapToOrder(orderSnap);

            // 2. Business Logic
            MissionAssignmentResult assignmentResult = assignmentLogic.apply(new DroneOrderContext(drone, order));

            // 3. Writes
            if (assignmentResult.mission() != null) {
                DocumentReference missionRef = firestore.collection(appProperties.getMissionsCollection())
                        .document(assignmentResult.mission().getId());
                transaction.set(missionRef, mapper.mapFromMission(assignmentResult.mission()));
            }

            transaction.set(droneRef, mapper.mapFromDrone(assignmentResult.updatedDrone()));
            transaction.set(orderRef, mapper.mapFromOrder(assignmentResult.updatedOrder()));

            return assignmentResult.mission();
        });

        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Mission assignment transaction failed", e);
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
            throw new RuntimeException("Transaction failed", e);
        }
    }

    @Override
    public void runTelemetryUpdateTransaction(DroneTelemetry telemetry) {
        firestore.runTransaction(transaction -> {
            DocumentReference droneRef = firestore.collection(appProperties.getDronesCollection()).document(telemetry.droneId());
            DocumentSnapshot droneSnap = transaction.get(droneRef).get();

            Drone drone;
            if (droneSnap.exists()) {
                drone = mapper.mapToDrone(droneSnap);

                // Ordering check: if incoming telemetry is older than last update, skip
                if (drone.getLastUpdate() != null &&
                    telemetry.timestamp().toInstant().isBefore(drone.getLastUpdate())) {
                    log.info("Stale telemetry for drone {}. Current: {}, Incoming: {}. Skipping.",
                            drone.getId(), drone.getLastUpdate(), telemetry.timestamp());
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
                    telemetry.timestamp().toInstant()
            );

            transaction.set(droneRef, mapper.mapFromDrone(drone));
            return null;
        });
    }

    @Override
    public void runOrderIngestionTransaction(Order order) {
        firestore.runTransaction(transaction -> {
            DocumentReference orderRef = firestore.collection(appProperties.getOrdersCollection()).document(order.getId());
            DocumentSnapshot orderSnap = transaction.get(orderRef).get();

            if (orderSnap.exists()) {
                Order existing = mapper.mapToOrder(orderSnap);
                // If order already exists and is not PENDING, we don't want to reset it
                if (!"PENDING".equals(existing.getStatus()) && existing.getStatus() != null) {
                    log.info("Order {} already exists with status {}. Skipping ingestion.",
                            order.getId(), existing.getStatus());
                    return null;
                }
            }

            if (order.getStatus() == null) {
                order.setStatus("PENDING");
            }

            transaction.set(orderRef, mapper.mapFromOrder(order));
            return null;
        });
    }
}
