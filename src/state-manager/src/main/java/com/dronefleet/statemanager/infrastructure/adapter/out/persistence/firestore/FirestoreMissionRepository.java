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

    @Override
    public void save(Mission mission) {
        try {
            log.debug("Saving mission {} to Firestore...", mission.getId());
            firestore.collection(appProperties.getMissionsCollection())
                    .document(mission.getId())
                    .set(mapToDocument(mission))
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
                return Optional.of(mapToDomain(document));
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving mission from Firestore: {}", id, e);
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    private Mission mapToDomain(DocumentSnapshot doc) {
        List<Map<String, Object>> routeMaps = (List<Map<String, Object>>) doc.get("route");
        List<Position> route = null;
        if (routeMaps != null) {
            route = routeMaps.stream()
                    .map(this::mapPosition)
                    .collect(Collectors.toList());
        }

        return Mission.builder()
                .id(doc.getId())
                .droneId(doc.getString("droneId"))
                .orderId(doc.getString("orderId"))
                .route(route)
                .status(doc.getString("status"))
                .startTime(doc.getTimestamp("startTime") != null ? doc.getTimestamp("startTime").toDate().toInstant() : null)
                .endTime(doc.getTimestamp("endTime") != null ? doc.getTimestamp("endTime").toDate().toInstant() : null)
                .build();
    }

    private Map<String, Object> mapToDocument(Mission mission) {
        Map<String, Object> map = new HashMap<>();
        map.put("droneId", mission.getDroneId());
        map.put("orderId", mission.getOrderId());
        map.put("status", mission.getStatus());

        if (mission.getRoute() != null) {
            map.put("route", mission.getRoute().stream()
                    .map(this::mapPosition)
                    .collect(Collectors.toList()));
        }

        if (mission.getStartTime() != null) {
            map.put("startTime", Timestamp.of(java.util.Date.from(mission.getStartTime())));
        }
        if (mission.getEndTime() != null) {
            map.put("endTime", Timestamp.of(java.util.Date.from(mission.getEndTime())));
        }
        return map;
    }

    private Position mapPosition(Map<String, Object> map) {
        if (map == null) return null;
        return new Position(
                ((Number) map.get("lat")).doubleValue(),
                ((Number) map.get("lon")).doubleValue()
        );
    }

    private Map<String, Object> mapPosition(Position pos) {
        Map<String, Object> map = new HashMap<>();
        map.put("lat", pos.lat());
        map.put("lon", pos.lon());
        return map;
    }
}
