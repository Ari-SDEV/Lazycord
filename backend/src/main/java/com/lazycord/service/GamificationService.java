package com.lazycord.service;

import com.lazycord.model.User;
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

    private static final String[] RANKS = {
            "NEWBIE",
            "BRONZE",
            "SILVER",
            "GOLD",
            "PLATINUM",
            "DIAMOND",
            "MASTER",
            "LEGEND"
    };

    @Transactional
    public User addXp(User user, int xp) {
        user.setXp(user.getXp() + xp);

        int newLevel = calculateLevel(user.getXp());
        if (newLevel > user.getLevel()) {
            user.setLevel(newLevel);
            String newRank = calculateRank(newLevel);
            user.setRank(newRank);
            log.info("User {} leveled up to level {} and rank {}",
                    user.getUsername(), newLevel, newRank);
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

    public String calculateRank(int level) {
        int rankIndex = Math.min((level - 1) / 3, RANKS.length - 1);
        return RANKS[rankIndex];
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
