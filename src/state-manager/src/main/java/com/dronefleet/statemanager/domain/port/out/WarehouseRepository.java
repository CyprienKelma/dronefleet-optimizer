package com.dronefleet.statemanager.domain.port.out;

import com.dronefleet.statemanager.domain.model.Warehouse;
import java.util.Optional;
import java.util.List;

public interface WarehouseRepository {
    List<Warehouse> findAll();
    Optional<Warehouse> findById(String id);
    void save(Warehouse warehouse);
}
