package com.dronefleet.statemanager.domain.port.out;

import com.dronefleet.statemanager.domain.model.Order;
import java.util.Optional;
import java.util.List;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String id);
    List<Order> findPending();
}
