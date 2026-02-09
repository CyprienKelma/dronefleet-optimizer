package com.dronefleet.statemanager.domain.service;

import com.dronefleet.shared.models.Position;

/** Utility class for geospatial operations and validation. */
public class GeospatialUtils {

    /**
     * Check if coordinates are valid.
     *
     * @param position the position to validate
     * @return true if valid
     */
    public static boolean isValid(Position position) {
        if (position == null) {
            return false;
        }
        return position.getLat() >= -90
                && position.getLat() <= 90
                && position.getLon() >= -180
                && position.getLon() <= 180;
    }
}
