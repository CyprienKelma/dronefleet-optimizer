package com.dronefleet.statemanager.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * DTO for incoming order events from Pub/Sub.
 */
public record OrderEventDto(
    @JsonProperty("order_id") String orderId,
    @JsonProperty("priority") String priority,
    @JsonProperty("pickup_location") GeoPointDto pickupLocation,
    @JsonProperty("dropoff_location") GeoPointDto dropoffLocation,
    @JsonProperty("product_type") String productType,
    @JsonProperty("package_weight_kg") double packageWeightKg,
    @JsonProperty("content_description") String contentDescription,
    @JsonProperty("requires_cold_chain") boolean requiresColdChain,
    @JsonProperty("requester_id") String requesterId,
    @JsonProperty("created_at") OffsetDateTime createdAt
) {
    public record GeoPointDto(double lat, double lon) {}
}
