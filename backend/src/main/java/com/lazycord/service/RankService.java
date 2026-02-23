package com.lazycord.service;

import com.lazycord.model.Rank;
import com.lazycord.repository.RankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {

    private final RankRepository rankRepository;

    @Transactional(readOnly = true)
    public List<Rank> getAllRanks() {
        return rankRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public Rank getRankById(UUID id) {
        return rankRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rank not found"));
    }

    @Transactional(readOnly = true)
    public Rank getRankByLevel(int level) {
        return rankRepository.findByLevel(level)
                .orElseThrow(() -> new RuntimeException("No rank found for level " + level));
    }

    @Transactional
    public Rank createRank(Rank rank) {
        if (rankRepository.existsByName(rank.getName())) {
            throw new RuntimeException("Rank with name " + rank.getName() + " already exists");
        }
        
        // Validate level range doesn't overlap with existing ranks
        if (rankRepository.existsByMinLevelLessThanEqualAndMaxLevelGreaterThanEqual(
                rank.getMaxLevel(), rank.getMinLevel())) {
            throw new RuntimeException("Level range overlaps with existing rank");
        }

        log.info("Creating rank: {}", rank.getName());
        return rankRepository.save(rank);
    }

    @Transactional
    public Rank updateRank(UUID id, Rank updatedRank) {
        Rank rank = getRankById(id);
        
        // Check name uniqueness if changed
        if (!rank.getName().equals(updatedRank.getName()) && 
            rankRepository.existsByName(updatedRank.getName())) {
            throw new RuntimeException("Rank with name " + updatedRank.getName() + " already exists");
        }

        rank.setName(updatedRank.getName());
        rank.setDisplayName(updatedRank.getDisplayName());
        rank.setDescription(updatedRank.getDescription());
        rank.setMinLevel(updatedRank.getMinLevel());
        rank.setMaxLevel(updatedRank.getMaxLevel());
        rank.setBadgeUrl(updatedRank.getBadgeUrl());
        rank.setColorHex(updatedRank.getColorHex());
        rank.setSortOrder(updatedRank.getSortOrder());
        rank.setActive(updatedRank.isActive());

        log.info("Updating rank: {}", rank.getName());
        return rankRepository.save(rank);
    }

    @Transactional
    public void deleteRank(UUID id) {
        Rank rank = getRankById(id);
        rank.setActive(false);
        rankRepository.save(rank);
        log.info("Deactivated rank: {}", rank.getName());
    }

    @Transactional
    public void deleteRankPermanently(UUID id) {
        rankRepository.deleteById(id);
        log.info("Deleted rank: {}", id);
    }
}
