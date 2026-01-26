package com.dronefleet.statemanager.domain.port.out;

import com.dronefleet.statemanager.domain.model.Order;
import java.util.Optional;
import java.util.List;

public interface OrderRepository {

    /**
     * Save an order.
     * @param order the order to save
     */
    void save(Order order);

    /**
     * Find an order by its id.
     * @param id the id of the order
     * @return the order
     */
    Optional<Order> findById(String id);

    /**
     * Find all pending orders.
     * @return a list of pending orders
     */
    List<Order> findPending();
}
