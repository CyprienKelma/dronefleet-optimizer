package com.dronefleet.statemanager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {
    private String id;
    private String name;
    private Position position;
    private List<String> authorizedProductTypes;
    private boolean isColdStorageCapable;
}
