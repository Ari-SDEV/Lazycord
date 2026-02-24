package com.lazycord.service;

import com.lazycord.model.Community;
import com.lazycord.model.Mission;
import com.lazycord.model.MissionProgress;
import com.lazycord.model.User;
import com.lazycord.repository.MissionProgressRepository;
import com.lazycord.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionService {

    private final MissionRepository missionRepository;
    private final MissionProgressRepository missionProgressRepository;
    private final GamificationService gamificationService;

    @Transactional(readOnly = true)
    public List<Mission> getAvailableMissions(Community community) {
        return missionRepository.findAvailableMissionsByCommunity(community, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Mission> getActiveMissions(Community community) {
        return missionRepository.findByCommunityAndActiveTrue(community);
    }

    @Transactional(readOnly = true)
    public List<MissionProgress> getUserMissions(User user) {
        return missionProgressRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<MissionProgress> getUserActiveMissions(User user) {
        return missionProgressRepository.findByUserAndCompletedFalse(user);
    }

    @Transactional
    public void startMission(User user, UUID missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission not found"));

        if (missionProgressRepository.findByUserAndMission(user, mission).isPresent()) {
            throw new RuntimeException("Mission already started");
        }

        MissionProgress progress = new MissionProgress();
        progress.setUser(user);
        progress.setMission(mission);
        progress.setCurrentCount(0);
        progress.setCompleted(false);
        progress.setRewarded(false);

        missionProgressRepository.save(progress);
        log.info("User {} started mission {}", user.getUsername(), mission.getTitle());
    }

    @Transactional
    public void updateMissionProgress(User user, UUID missionId, int count) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission not found"));

        MissionProgress progress = missionProgressRepository.findByUserAndMission(user, mission)
                .orElseThrow(() -> new RuntimeException("Mission not started"));

        if (progress.isCompleted()) {
            return;
        }

        progress.setCurrentCount(Math.min(count, mission.getRequiredCount()));

        if (progress.getCurrentCount() >= mission.getRequiredCount()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            log.info("User {} completed mission {}", user.getUsername(), mission.getTitle());
        }

        missionProgressRepository.save(progress);
    }

    @Transactional
    public void incrementMissionProgress(User user, UUID missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission not found"));

        MissionProgress progress = missionProgressRepository.findByUserAndMission(user, mission)
                .orElseThrow(() -> new RuntimeException("Mission not started"));

        if (progress.isCompleted()) {
            return;
        }

        progress.setCurrentCount(progress.getCurrentCount() + 1);

        if (progress.getCurrentCount() >= mission.getRequiredCount()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }

        missionProgressRepository.save(progress);

        if (progress.isCompleted()) {
            log.info("User {} completed mission {}", user.getUsername(), mission.getTitle());
        }
    }

    @Transactional
    public void claimMissionReward(User user, UUID missionId) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new RuntimeException("Mission not found"));

        MissionProgress progress = missionProgressRepository.findByUserAndMission(user, mission)
                .orElseThrow(() -> new RuntimeException("Mission not found"));

        if (!progress.isCompleted()) {
            throw new RuntimeException("Mission not completed");
        }

        if (progress.isRewarded()) {
            throw new RuntimeException("Reward already claimed");
        }

        progress.setRewarded(true);
        missionProgressRepository.save(progress);

        gamificationService.addXp(user, mission.getXpReward());
        gamificationService.addPoints(user, mission.getPointsReward());

        log.info("User {} claimed reward for mission {}: {} XP, {} Points",
                user.getUsername(), mission.getTitle(), mission.getXpReward(), mission.getPointsReward());
    }

    @Transactional(readOnly = true)
    public List<MissionProgress> getCompletedUnrewardedMissions(User user) {
        return missionProgressRepository.findCompletedUnrewardedByUser(user);
    }
}
