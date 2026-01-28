package com.dronefleet.statemanager.application.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** DTO for incoming mission assignment decisions from the Optimizer. */
public record MissionAssignmentDto(
        @JsonProperty("drone_id") String droneId,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("route") List<GeoPointDto> route) {
    public record GeoPointDto(double lat, double lon) {}
}
