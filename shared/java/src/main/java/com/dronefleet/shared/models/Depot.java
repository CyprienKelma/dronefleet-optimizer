package com.dronefleet.shared.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Depot {
    private String id;
    private String name;
    private Position position;
    private int capacity;
    private int chargingSlots;
}
