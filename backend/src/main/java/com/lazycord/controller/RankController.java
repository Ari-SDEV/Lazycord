package com.lazycord.controller;

import com.lazycord.model.Rank;
import com.lazycord.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ranks")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:1420", "tauri://localhost"})
public class RankController {

    private final RankService rankService;

    @GetMapping
    public ResponseEntity<List<Rank>> getAllRanks() {
        return ResponseEntity.ok(rankService.getAllRanks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rank> getRankById(@PathVariable UUID id) {
        return ResponseEntity.ok(rankService.getRankById(id));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<Rank> getRankByLevel(@PathVariable int level) {
        return ResponseEntity.ok(rankService.getRankByLevel(level));
    }

    @PostMapping
    public ResponseEntity<Rank> createRank(@RequestBody Rank rank) {
        return ResponseEntity.ok(rankService.createRank(rank));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Rank> updateRank(@PathVariable UUID id, @RequestBody Rank rank) {
        return ResponseEntity.ok(rankService.updateRank(id, rank));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRank(@PathVariable UUID id) {
        rankService.deleteRank(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> deleteRankPermanently(@PathVariable UUID id) {
        rankService.deleteRankPermanently(id);
        return ResponseEntity.ok().build();
    }
}
