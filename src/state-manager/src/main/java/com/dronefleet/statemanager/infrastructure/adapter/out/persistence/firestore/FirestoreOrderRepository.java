package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.model.Order;
import com.dronefleet.statemanager.domain.model.Position;
import com.dronefleet.statemanager.domain.port.out.OrderRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
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
public class FirestoreOrderRepository implements OrderRepository {

    private final Firestore firestore;
    private final AppProperties appProperties;

    @Override
    public void save(Order order) {
        try {
            log.debug("Saving order {} to Firestore...", order.getId());
            firestore.collection(appProperties.getOrdersCollection())
                    .document(order.getId())
                    .set(mapToDocument(order))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving order to Firestore: {}", order.getId(), e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Optional<Order> findById(String id) {
        try {
            DocumentSnapshot document = firestore.collection(appProperties.getOrdersCollection())
                    .document(id)
                    .get()
                    .get();
            if (document.exists()) {
                return Optional.of(mapToDomain(document));
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving order from Firestore: {}", id, e);
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    @Override
    public List<Order> findPending() {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(appProperties.getOrdersCollection())
                    .whereEqualTo("status", "PENDING")
                    .get();
            return future.get().getDocuments().stream()
                    .map(this::mapToDomain)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving pending orders from Firestore", e);
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    private Order mapToDomain(DocumentSnapshot doc) {
        return Order.builder()
                .id(doc.getId())
                .pickupLocation(mapPosition((Map<String, Object>) doc.get("pickupLocation")))
                .deliveryLocation(mapPosition((Map<String, Object>) doc.get("deliveryLocation")))
                .status(doc.getString("status"))
                .priority(doc.getString("priority"))
                .createdAt(doc.getTimestamp("createdAt") != null ? doc.getTimestamp("createdAt").toDate().toInstant() : null)
                .build();
    }

    private Map<String, Object> mapToDocument(Order order) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", order.getStatus());
        map.put("priority", order.getPriority());
        if (order.getPickupLocation() != null) {
            map.put("pickupLocation", mapPosition(order.getPickupLocation()));
        }
        if (order.getDeliveryLocation() != null) {
            map.put("deliveryLocation", mapPosition(order.getDeliveryLocation()));
        }
        if (order.getCreatedAt() != null) {
            map.put("createdAt", Timestamp.of(java.util.Date.from(order.getCreatedAt())));
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
