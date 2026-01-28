package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import com.dronefleet.statemanager.domain.model.Warehouse;
import com.dronefleet.statemanager.domain.port.out.WarehouseRepository;
import com.dronefleet.statemanager.application.config.AppProperties;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FirestoreWarehouseRepository implements WarehouseRepository {

    private final Firestore firestore;
    private final AppProperties appProperties;
    private final FirestoreMapper mapper;

    @Override
    public List<Warehouse> findAll() {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection(appProperties.getWarehousesCollection()).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            return documents.stream()
                    .map(mapper::mapToWarehouse)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving all warehouses from Firestore", e);
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    @Override
    public Optional<Warehouse> findById(String id) {
        try {
            DocumentSnapshot document = firestore.collection(appProperties.getWarehousesCollection()).document(id).get().get();
            if (document.exists()) {
                return Optional.of(mapper.mapToWarehouse(document));
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving warehouse from Firestore: {}", id, e);
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    @Override
    public void save(Warehouse warehouse) {
        try {
            log.debug("Saving warehouse {} to Firestore...", warehouse.getId());
            firestore.collection(appProperties.getWarehousesCollection())
                    .document(warehouse.getId())
                    .set(mapper.mapFromWarehouse(warehouse))
                    .get();
            log.debug("Warehouse {} saved successfully.", warehouse.getId());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving warehouse to Firestore: {}", warehouse.getId(), e);
            Thread.currentThread().interrupt();
        }
    }
}
