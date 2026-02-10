package com.dronefleet.statemanager.domain.service;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.dronefleet.shared.models.Mission;
import com.dronefleet.statemanager.domain.port.in.GetMissionsUseCase;
import com.dronefleet.statemanager.domain.port.out.MissionRepository;

/**
 * Domain service for querying missions.
 *
 * <p>Implements the {@link GetMissionsUseCase} inbound port by delegating to the {@link
 * MissionRepository} outbound port.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionQueryService implements GetMissionsUseCase {

    private final MissionRepository missionRepository;

    @Override
    public List<Mission> getAllMissions() {
        log.info("Fetching all missions");
        List<Mission> missions = missionRepository.findAll();
        log.info("Retrieved {} missions", missions.size());
        return missions;
    }

    @Override
    public Optional<Mission> getMissionById(String id) {
        log.info("Fetching mission by id: {}", id);
        return missionRepository.findById(id);
    }
}
