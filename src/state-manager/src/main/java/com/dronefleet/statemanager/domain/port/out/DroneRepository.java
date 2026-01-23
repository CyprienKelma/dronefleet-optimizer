package com.dronefleet.statemanager.domain.port.out;

import com.dronefleet.statemanager.domain.model.Drone;
import java.util.Optional;
import java.util.List;

public interface DroneRepository {
    void save(Drone drone);
    Optional<Drone> findById(String id);
    List<Drone> findAll();
}
