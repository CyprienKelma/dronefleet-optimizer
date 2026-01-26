package com.dronefleet.statemanager.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for incoming mission assignment decisions from the Optimizer.
 */
public record MissionAssignmentDto(
    @JsonProperty("drone_id") String droneId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("route") List<GeoPointDto> route
) {
    public record GeoPointDto(double lat, double lon) {}
}
