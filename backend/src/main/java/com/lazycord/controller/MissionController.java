package com.lazycord.controller;

import com.lazycord.model.Mission;
import com.lazycord.model.MissionProgress;
import com.lazycord.model.User;
import com.lazycord.service.MissionService;
import com.lazycord.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:1420", "tauri://localhost"})
public class MissionController {

    private final MissionService missionService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<Mission>> getAvailableMissions() {
        return ResponseEntity.ok(missionService.getAvailableMissions());
    }

    @GetMapping("/my")
    public ResponseEntity<List<MissionProgress>> getMyMissions(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(missionService.getUserMissions(user));
    }

    @GetMapping("/active")
    public ResponseEntity<List<MissionProgress>> getMyActiveMissions(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(missionService.getUserActiveMissions(user));
    }

    @PostMapping("/{missionId}/start")
    public ResponseEntity<Void> startMission(@PathVariable UUID missionId, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        missionService.startMission(user, missionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{missionId}/claim")
    public ResponseEntity<Void> claimReward(@PathVariable UUID missionId, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        missionService.claimMissionReward(user, missionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/completed/unrewarded")
    public ResponseEntity<List<MissionProgress>> getUnrewardedMissions(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(missionService.getCompletedUnrewardedMissions(user));
    }
}
