package com.dronefleet.statemanager.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.dronefleet.shared.models.Warehouse;

public interface WarehouseRepository {
    List<Warehouse> findAll();

    Optional<Warehouse> findById(String id);

    void save(Warehouse warehouse);
}
