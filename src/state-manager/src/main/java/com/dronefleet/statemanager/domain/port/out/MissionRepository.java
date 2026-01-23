package com.dronefleet.statemanager.domain.port.out;

import com.dronefleet.statemanager.domain.model.Mission;
import java.util.Optional;

public interface MissionRepository {
    void save(Mission mission);
    Optional<Mission> findById(String id);
}
