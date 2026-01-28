package com.dronefleet.statemanager.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.dronefleet.statemanager.domain.model.Drone;

public interface DroneRepository {

    /**
     * Save a drone.
     *
     * @param drone The drone to save.
     */
    void saveDrone(Drone drone);

    /**
     * Find a drone by id.
     *
     * @param id The id of the drone to find.
     * @return The drone if found, otherwise Optional.empty().
     */
    Optional<Drone> findById(String id);

    /**
     * Find all drones.
     *
     * @return A list of all drones.
     */
    List<Drone> findAll();

    /**
     * Find drones that are IDLE and have battery percentage above threshold.
     *
     * @param minBatteryPercent The minimum battery percentage.
     * @return A list of available drones.
     */
    List<Drone> findAvailableForOptimization(int minBatteryPercent);
}
