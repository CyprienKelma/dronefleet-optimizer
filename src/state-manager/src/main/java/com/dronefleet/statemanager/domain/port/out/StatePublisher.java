package com.dronefleet.statemanager.domain.port.out;

public interface StatePublisher {
    void publishDroneUpdate(String droneId, String status);
}
