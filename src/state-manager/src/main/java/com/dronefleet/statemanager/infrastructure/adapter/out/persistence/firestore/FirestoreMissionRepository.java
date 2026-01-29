package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.model.Mission;
import com.dronefleet.statemanager.domain.port.out.MissionRepository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FirestoreMissionRepository implements MissionRepository {

    private final Firestore firestore;
    private final AppProperties appProperties;
    private final FirestoreMapper mapper;

    @Override
    public void save(Mission mission) {
        try {
            log.debug("Saving mission {} to Firestore...", mission.getId());
            firestore
                    .collection(appProperties.getMissionsCollection())
                    .document(mission.getId())
                    .set(mapper.mapFromMission(mission))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving mission to Firestore: {}", mission.getId(), e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Optional<Mission> findById(String id) {
        try {
            DocumentSnapshot document =
                    firestore
                            .collection(appProperties.getMissionsCollection())
                            .document(id)
                            .get()
                            .get();
            if (document.exists()) {
                return Optional.of(mapper.mapToMission(document));
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving mission from Firestore: {}", id, e);
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }
}
