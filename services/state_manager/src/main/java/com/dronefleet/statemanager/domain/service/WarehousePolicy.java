package com.dronefleet.statemanager.domain.service;

import org.springframework.stereotype.Component;

import com.dronefleet.shared.models.Order;
import com.dronefleet.shared.models.Warehouse;

@Component
public class WarehousePolicy {

    /** Check if warehouse can fulfill order product type. */
    public boolean canFulfillOrder(Warehouse warehouse, Order order) {
        return warehouse.getAuthorizedProductTypesList().contains(order.getProductType());
    }
}
