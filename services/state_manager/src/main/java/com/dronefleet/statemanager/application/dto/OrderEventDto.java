package com.dronefleet.statemanager.application.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/** DTO for incoming order events from Pub/Sub. */
public record OrderEventDto(
        @JsonProperty("order_id") @JsonAlias({"orderId", "id"}) String orderId,
        @JsonProperty("priority") String priority,
        @JsonProperty("pickup_location") @JsonAlias("pickupLocation") GeoPointDto pickupLocation,
        @JsonProperty("delivery_location")
                @JsonAlias({"deliveryLocation", "dropoff_location", "dropoffLocation"})
                GeoPointDto dropoffLocation,
        @JsonProperty("product_type") @JsonAlias("productType") String productType,
        @JsonProperty("package_weight_kg") @JsonAlias("packageWeightKg") double packageWeightKg,
        @JsonProperty("content_description") @JsonAlias("contentDescription")
                String contentDescription,
        @JsonProperty("requires_cold_chain") @JsonAlias("requiresColdChain")
                boolean requiresColdChain,
        @JsonProperty("requester_id") @JsonAlias("requesterId") String requesterId,
        @JsonProperty("created_at") @JsonAlias("createdAt") OffsetDateTime createdAt) {
    public record GeoPointDto(double lat, double lon) {}
}
