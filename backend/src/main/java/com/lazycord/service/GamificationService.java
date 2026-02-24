package com.lazycord.service;

import com.lazycord.model.Rank;
import com.lazycord.model.User;
import com.lazycord.repository.RankRepository;
import com.lazycord.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final UserRepository userRepository;
    private final RankRepository rankRepository;

    private static final int[] XP_LEVELS = {
            0,       // Level 1
            100,     // Level 2
            250,     // Level 3
            500,     // Level 4
            1000,    // Level 5
            2000,    // Level 6
            3500,    // Level 7
            5500,    // Level 8
            8000,    // Level 9
            11000,   // Level 10
            15000,   // Level 11
            20000,   // Level 12
            26000,   // Level 13
            33000,   // Level 14
            41000,   // Level 15
            50000,   // Level 16
            60000,   // Level 17
            72000,   // Level 18
            85000,   // Level 19
            100000   // Level 20
    };

    @Transactional
    public User addXp(User user, int xp, Community community) {
        user.setXp(user.getXp() + xp);

        int newLevel = calculateLevel(user.getXp());
        if (newLevel > user.getLevel()) {
            user.setLevel(newLevel);
            Rank newRank = calculateRank(newLevel, community);
            if (newRank != null) {
                user.setRank(newRank.getName());
                log.info("User {} leveled up to level {} and rank {} in community {}",
                        user.getUsername(), newLevel, newRank.getDisplayName(), community.getName());
            }
        }

        return userRepository.save(user);
    }

    @Transactional
    public User addPoints(User user, int points) {
        user.setPoints(user.getPoints() + points);
        return userRepository.save(user);
    }

    @Transactional
    public User deductPoints(User user, int points) {
        if (user.getPoints() < points) {
            throw new RuntimeException("Insufficient points");
        }
        user.setPoints(user.getPoints() - points);
        return userRepository.save(user);
    }

    public int calculateLevel(int totalXp) {
        int level = 1;
        for (int i = 1; i < XP_LEVELS.length; i++) {
            if (totalXp >= XP_LEVELS[i]) {
                level = i + 1;
            } else {
                break;
            }
        }
        return level;
    }

    public Rank calculateRank(int level, Community community) {
        return rankRepository.findHighestRankForLevelInCommunity(community, level)
                .orElse(null);
    }

    public Rank getRankForLevel(int level, Community community) {
        return rankRepository.findByCommunityAndLevel(community, level)
                .orElse(null);
    }

    public int getXpForNextLevel(int currentLevel) {
        if (currentLevel >= XP_LEVELS.length) {
            return XP_LEVELS[XP_LEVELS.length - 1] + 50000;
        }
        return XP_LEVELS[currentLevel];
    }

    public int getProgressToNextLevel(User user) {
        int currentLevel = user.getLevel();
        int xpForCurrentLevel = currentLevel > 1 ? XP_LEVELS[currentLevel - 1] : 0;
        int xpForNextLevel = getXpForNextLevel(currentLevel);
        int currentXp = user.getXp();

        return (int) (((double) (currentXp - xpForCurrentLevel) / (xpForNextLevel - xpForCurrentLevel)) * 100);
    }
}
