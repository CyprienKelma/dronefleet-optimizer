package com.dronefleet.statemanager.domain.model;

/**
 * Represents the lifecycle stages of a delivery order.
 */
public enum OrderStatus {
    /** Order has been received but not yet processed by the optimizer. */
    PENDING,
    /** Order is currently being processed by an optimization session. */
    SOLVING,
    /** Order has been assigned to a drone/mission. */
    ASSIGNED,
    /** Order is currently in the delivery phase (onboard a drone). */
    IN_DELIVERY,
    /** Order has been successfully delivered to the customer. */
    DELIVERED,
    /** Order has been cancelled. */
    CANCELLED;

    /**
     * Parse a status string to an OrderStatus enum.
     * @param statusStr the status string to parse
     * @return the OrderStatus enum
     */
    public static OrderStatus parseStatus(String statusStr) {
        if (statusStr == null) return PENDING;
        try {
            return OrderStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
