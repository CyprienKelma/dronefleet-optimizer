package com.dronefleet.shared.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
