package com.dronefleet.statemanager.domain.service;

import com.dronefleet.statemanager.domain.port.in.AssignMissionUseCase;
import com.dronefleet.statemanager.domain.port.out.MissionRepository;
import com.dronefleet.statemanager.domain.model.Mission;
import com.dronefleet.statemanager.domain.model.Position;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MissionCreationService implements AssignMissionUseCase {
    private final MissionRepository missionRepository;

    @Autowired
    public MissionCreationService(MissionRepository missionRepository) {
        this.missionRepository = missionRepository;
    }

    @Override
    public void assignMission(String droneId, String orderId, List<Position> route) {
        log.info("Creating mission {}", mission.getId());
        missionRepository.save(mission);
        log.info("Mission {} created", mission.getId());
    }
}
