package com.dronefleet.statemanager.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.dronefleet.shared.models.Order;

public interface OrderRepository {

    /**
     * Save an order.
     *
     * @param order the order to save
     */
    void save(Order order);

    /**
     * Find an order by its id.
     *
     * @param id the id of the order
     * @return the order
     */
    Optional<Order> findById(String id);

    /**
     * Find all pending orders.
     *
     * @return a list of pending orders
     */
    List<Order> findPending();
}
