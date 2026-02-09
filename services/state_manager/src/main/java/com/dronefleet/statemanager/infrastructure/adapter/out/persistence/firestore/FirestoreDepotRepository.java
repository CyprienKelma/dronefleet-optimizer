package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.dronefleet.shared.models.Depot;
import com.dronefleet.statemanager.application.config.AppProperties;
import com.dronefleet.statemanager.domain.port.out.DepotRepository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FirestoreDepotRepository implements DepotRepository {

    private final Firestore firestore;
    private final AppProperties appProperties;
    private final FirestoreMapper mapper;

    @Override
    public Optional<Depot> findMainDepot() {
        try {
            // For now, we assume there's only one main depot or we take the first one
            QuerySnapshot querySnapshot =
                    firestore.collection(appProperties.getDepotsCollection()).limit(1).get().get();

            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                return Optional.of(mapper.mapToDepot(document));
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving main depot from Firestore", e);
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    @Override
    public Depot save(Depot depot) {
        try {
            firestore
                    .collection(appProperties.getDepotsCollection())
                    .document(depot.getId())
                    .set(mapper.mapFromDepot(depot))
                    .get();
            return depot;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error saving depot to Firestore: {}", depot.getId(), e);
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
