package com.dronefleet.statemanager.domain.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum DroneStatus {
    IDLE,
    MOVING,
    DELIVERING,
    CHARGING,
    MAINTENANCE,
    PROBLEM,
    EMERGENCY,
    RESERVED,
    UNKNOWN;


    /**
     * Parse a status string to a DroneStatus enum.
     * @param statusStr the status string to parse
     * @return the DroneStatus enum
     */
    public static DroneStatus parseStatus(String statusStr) {
        try {
            return DroneStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Status unknown : '{}', using UNKNOWN by default", statusStr);
            return DroneStatus.UNKNOWN;
        }
    }

    /**
     * Check if the drone is operational.
     * Business rule: A drone is operational if it's IDLE or MOVING.
     * @return true if the drone is operational, false otherwise
     */
    public boolean isOperational() {
        return this == IDLE || this == MOVING;
    }

    /**
     * Check if the drone requires immediate attention.
     * Business rule: MAINTENANCE and PROBLEM states require intervention.
     * @return true if the drone requires attention, false otherwise
     */
    public boolean requiresAttention() {
        return this == MAINTENANCE || this == PROBLEM;
    }

    /**
     * Check if the drone can accept a new mission.
     * Business rule: Only IDLE drones can be assigned new missions.
     * @return true if the drone can accept a new mission, false otherwise
     */
    public boolean canAcceptMission() {
        return this == IDLE;
    }
}
