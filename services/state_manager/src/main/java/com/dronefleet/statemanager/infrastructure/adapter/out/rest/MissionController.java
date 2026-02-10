package com.dronefleet.statemanager.infrastructure.adapter.out.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dronefleet.shared.models.Mission;
import com.dronefleet.statemanager.domain.port.in.GetMissionsUseCase;

@RestController
@RequestMapping("/api/v1/missions")
public class MissionController {

    private final GetMissionsUseCase getMissionsUseCase;

    @GetMapping("/missions")
    public List<Mission> getMissions() {
        return getMissionsUseCase.getMissions();
    }
}
