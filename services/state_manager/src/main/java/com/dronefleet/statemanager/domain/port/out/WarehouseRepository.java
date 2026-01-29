package com.dronefleet.statemanager.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.dronefleet.statemanager.domain.model.Warehouse;

public interface WarehouseRepository {
    List<Warehouse> findAll();

    Optional<Warehouse> findById(String id);

    void save(Warehouse warehouse);
}
