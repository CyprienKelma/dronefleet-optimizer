package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.model.Mission;
import com.dronefleet.statemanager.domain.model.Position;
import com.dronefleet.statemanager.domain.port.out.MissionRepository;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
            firestore.collection(appProperties.getMissionsCollection())
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
            DocumentSnapshot document = firestore.collection(appProperties.getMissionsCollection())
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
