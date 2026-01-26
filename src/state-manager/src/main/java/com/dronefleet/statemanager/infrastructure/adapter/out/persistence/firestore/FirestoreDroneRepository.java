package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import com.dronefleet.statemanager.domain.model.Drone;
import com.dronefleet.statemanager.domain.model.DroneStatus;
import com.dronefleet.statemanager.domain.model.Position;
import com.dronefleet.statemanager.domain.port.out.DroneRepository;
import com.dronefleet.statemanager.application.config.AppProperties;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
            DocumentSnapshot document = firestore.collection(appProperties.getDronesCollection()).document(id).get().get();
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
            ApiFuture<QuerySnapshot> future = firestore.collection(appProperties.getDronesCollection()).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            return documents.stream()
                    .map(mapper::mapToDrone)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving all drones from Firestore", e);
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    @Override
    public void saveDrone(Drone drone) {
        try {
            log.debug("Saving drone {} to Firestore...", drone.getId());
            firestore.collection(appProperties.getDronesCollection()).document(drone.getId()).set(mapper.mapFromDrone(drone)).get();
            log.debug("Drone {} saved successfully.", drone.getId());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving drone to Firestore: {}", drone.getId(), e);
            Thread.currentThread().interrupt();
        }
    }
}
