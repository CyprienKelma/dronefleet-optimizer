package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.model.Order;
import com.dronefleet.statemanager.domain.port.out.OrderRepository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FirestoreOrderRepository implements OrderRepository {

    private final Firestore firestore;
    private final AppProperties appProperties;
    private final FirestoreMapper mapper;

    @Override
    public void save(Order order) {
        try {
            log.debug("Saving order {} to Firestore...", order.getId());
            firestore
                    .collection(appProperties.getOrdersCollection())
                    .document(order.getId())
                    .set(mapper.mapFromOrder(order))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving order to Firestore: {}", order.getId(), e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Optional<Order> findById(String id) {
        try {
            DocumentSnapshot document =
                    firestore
                            .collection(appProperties.getOrdersCollection())
                            .document(id)
                            .get()
                            .get();
            if (document.exists()) {
                return Optional.of(mapper.mapToOrder(document));
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
            ApiFuture<QuerySnapshot> future =
                    firestore
                            .collection(appProperties.getOrdersCollection())
                            .whereEqualTo("status", "PENDING")
                            .get();
            return future.get().getDocuments().stream()
                    .map(mapper::mapToOrder)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving pending orders from Firestore", e);
            Thread.currentThread().interrupt();
            return List.of();
        }
    }
}
