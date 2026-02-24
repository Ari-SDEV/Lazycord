package com.lazycord.service;

import com.lazycord.model.*;
import com.lazycord.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RankRepository rankRepository;

    @InjectMocks
    private GamificationService gamificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setLevel(1);
        testUser.setXp(0);
        testUser.setPoints(100);
        testUser.setRank("NEWBIE");
    }

    @Test
    void addXp_LevelUp() {
        // Arrange
        testUser.setXp(90);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = gamificationService.addXp(testUser, 20);

        // Assert
        assertEquals(110, result.getXp());
        assertEquals(2, result.getLevel()); // Level up from 1 to 2
    }

    @Test
    void addXp_NoLevelUp() {
        // Arrange
        testUser.setXp(50);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = gamificationService.addXp(testUser, 20);

        // Assert
        assertEquals(70, result.getXp());
        assertEquals(1, result.getLevel()); // No level up
    }

    @Test
    void addPoints_Success() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = gamificationService.addPoints(testUser, 50);

        // Assert
        assertEquals(150, result.getPoints());
    }

    @Test
    void deductPoints_Success() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = gamificationService.deductPoints(testUser, 30);

        // Assert
        assertEquals(70, result.getPoints());
    }

    @Test
    void deductPoints_InsufficientPoints_ThrowsException() {
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            gamificationService.deductPoints(testUser, 200);
        });
    }

    @Test
    void calculateLevel_Xp0_ReturnsLevel1() {
        assertEquals(1, gamificationService.calculateLevel(0));
    }

    @Test
    void calculateLevel_Xp100_ReturnsLevel2() {
        assertEquals(2, gamificationService.calculateLevel(100));
    }

    @Test
    void calculateLevel_Xp100000_ReturnsLevel20() {
        assertEquals(20, gamificationService.calculateLevel(100000));
    }

    @Test
    void getProgressToNextLevel_Level1_Xp50_Returns50Percent() {
        // Arrange
        testUser.setLevel(1);
        testUser.setXp(50);

        // Act
        int progress = gamificationService.getProgressToNextLevel(testUser);

        // Assert
        assertEquals(50, progress);
    }

    @Test
    void getProgressToNextLevel_Level5_Xp1500_ReturnsProgress() {
        // Arrange
        testUser.setLevel(5);
        testUser.setXp(1500);

        // Act
        int progress = gamificationService.getProgressToNextLevel(testUser);

        // Assert
        assertTrue(progress >= 0 && progress <= 100);
    }
}
