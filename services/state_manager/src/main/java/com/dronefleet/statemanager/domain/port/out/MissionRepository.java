package com.dronefleet.statemanager.domain.port.out;

import java.util.Optional;

import com.dronefleet.shared.models.Mission;

public interface MissionRepository {
    void save(Mission mission);

    Optional<Mission> findById(String id);
}
