package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.dronefleet.shared.models.Drone;
import com.dronefleet.shared.models.DroneStatus;
import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.port.out.DroneRepository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FirestoreDroneRepository implements DroneRepository {

    private final Firestore firestore;
    private final AppProperties appProperties;
    private final FirestoreMapper mapper;

    @Override
    public Optional<Drone> findById(String id) {
        try {
            DocumentSnapshot document =
                    firestore
                            .collection(appProperties.getDronesCollection())
                            .document(id)
                            .get()
                            .get();
            if (document.exists()) {
                return Optional.of(mapper.mapToDrone(document));
            }
            // if the thread is interrupted or error occurs during Firestore query
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving drone from Firestore: {}", id, e);
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    @Override
    public List<Drone> findAll() {
        try {
            ApiFuture<QuerySnapshot> future =
                    firestore.collection(appProperties.getDronesCollection()).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            return documents.stream().map(mapper::mapToDrone).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving all drones from Firestore", e);
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    @Override
    public List<Drone> findAvailableForOptimization(int minBatteryPercent) {
        String collection = appProperties.getDronesCollection();
        log.debug(
                "Querying collection: '{}' for idle drones with battery >= {}%",
                collection, minBatteryPercent);
        try {
            ApiFuture<QuerySnapshot> future =
                    firestore
                            .collection(collection)
                            .whereEqualTo("status", DroneStatus.DRONE_STATUS_IDLE.name())
                            .whereGreaterThanOrEqualTo(
                                    "batteryPercentage", (double) minBatteryPercent)
                            .get();
            return future.get(10, java.util.concurrent.TimeUnit.SECONDS).getDocuments().stream()
                    .map(mapper::mapToDrone)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error(
                    "Error retrieving available drones from Firestore (collection: '{}')",
                    collection,
                    e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    @Override
    public void saveDrone(Drone drone) {
        try {
            log.debug(
                    "Saving drone {} to Firestore (collection: '{}')...",
                    drone.getId(),
                    appProperties.getDronesCollection());
            firestore
                    .collection(appProperties.getDronesCollection())
                    .document(drone.getId())
                    .set(mapper.mapFromDrone(drone));
            log.debug("Drone update requested for {}.", drone.getId());
        } catch (Exception e) {
            log.error(
                    "Error initiating save drone to Firestore (collection: '{}'): {}",
                    appProperties.getDronesCollection(),
                    drone.getId(),
                    e);
        }
    }
}
