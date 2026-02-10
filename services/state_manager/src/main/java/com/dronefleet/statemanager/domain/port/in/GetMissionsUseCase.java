package com.dronefleet.statemanager.domain.port.in;

import java.util.List;
import java.util.Optional;

import com.dronefleet.shared.models.Mission;

/**
 * Inbound port for querying missions.
 *
 * <p>Used by REST controllers to retrieve mission data produced by the optimizer.
 */
public interface GetMissionsUseCase {

    /** Retrieve all missions. */
    List<Mission> getAllMissions();

    /** Retrieve a single mission by its identifier. */
    Optional<Mission> getMissionById(String id);
}
