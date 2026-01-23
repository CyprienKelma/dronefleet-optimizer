package com.dronefleet.statemanager.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Mission {
    private String id;
    private String droneId;
    private String orderId;
    private List<Position> route;
    private String status; // ACTIVE, COMPLETED, FAILED
    private Instant startTime;
    private Instant endTime;
}
