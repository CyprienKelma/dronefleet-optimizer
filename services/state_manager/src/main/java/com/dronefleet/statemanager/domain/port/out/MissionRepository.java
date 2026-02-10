package com.dronefleet.statemanager.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.dronefleet.shared.models.Mission;

public interface MissionRepository {
    void save(Mission mission);

    Optional<Mission> findById(String id);

    /** Retrieve all missions from the data store. */
    List<Mission> findAll();
}
