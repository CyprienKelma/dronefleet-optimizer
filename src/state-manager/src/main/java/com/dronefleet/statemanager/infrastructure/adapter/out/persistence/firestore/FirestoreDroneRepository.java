package com.dronefleet.statemanager.infrastructure.adapter.out.persistence.firestore;

import com.dronefleet.statemanager.domain.model.Drone;
import com.dronefleet.statemanager.domain.port.out.DroneRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class FirestoreDroneRepository implements DroneRepository {
    private final ConcurrentHashMap<String, Drone> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<Drone> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Drone> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void save(Drone drone) {
        storage.put(drone.getId(), drone);
    }
}
