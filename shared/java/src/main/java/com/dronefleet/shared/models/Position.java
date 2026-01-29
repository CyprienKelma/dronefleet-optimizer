package com.dronefleet.shared.models;

/** Record representing a geographic position. */
public record Position(double lat, double lon) {
    /** Business rule: Check if coordinates are valid. */
    public boolean isValid() {
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }
}
