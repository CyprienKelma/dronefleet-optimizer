package com.dronefleet.statemanager.domain.port.out;

import java.util.Optional;

import com.dronefleet.shared.models.Depot;

public interface DepotRepository {
    Optional<Depot> findMainDepot();

    Depot save(Depot depot);
}
