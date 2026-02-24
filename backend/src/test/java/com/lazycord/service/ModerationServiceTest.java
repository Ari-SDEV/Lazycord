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
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private ChannelBanRepository banRepository;

    @Mock
    private ChannelMuteRepository muteRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelMemberRepository channelMemberRepository;

    @InjectMocks
    private ModerationService moderationService;

    private User testUser;
    private User adminUser;
    private Channel testChannel;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setUsername("admin");

        testChannel = new Channel();
        testChannel.setId(UUID.randomUUID());
        testChannel.setName("test-channel");
    }

    @Test
    void banUser_Success() {
        // Arrange
        when(banRepository.isUserBanned(testUser, testChannel, LocalDateTime.now())).thenReturn(false);
        when(banRepository.save(any(ChannelBan.class))).thenAnswer(invocation -> {
            ChannelBan ban = invocation.getArgument(0);
            ban.setId(1L);
            return ban;
        });

        // Act
        moderationService.banUser(testUser, testChannel, "Spam", adminUser,
                ModerationService.Duration.hours(24));

        // Assert
        verify(banRepository, times(1)).save(any(ChannelBan.class));
        verify(channelMemberRepository, times(1)).delete(any());
    }

    @Test
    void banUser_AlreadyBanned_ThrowsException() {
        // Arrange
        when(banRepository.isUserBanned(testUser, testChannel, LocalDateTime.now())).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            moderationService.banUser(testUser, testChannel, "Spam", adminUser,
                    ModerationService.Duration.permanent());
        });
    }

    @Test
    void muteUser_Success() {
        // Arrange
        when(muteRepository.isUserMuted(testUser, testChannel, LocalDateTime.now())).thenReturn(false);
        when(muteRepository.save(any(ChannelMute.class))).thenAnswer(invocation -> {
            ChannelMute mute = invocation.getArgument(0);
            mute.setId(1L);
            return mute;
        });

        // Act
        moderationService.muteUser(testUser, testChannel, "Inappropriate language", adminUser,
                ModerationService.Duration.hours(1));

        // Assert
        verify(muteRepository, times(1)).save(any(ChannelMute.class));
    }

    @Test
    void isUserBanned_ActiveBan_ReturnsTrue() {
        // Arrange
        when(banRepository.isUserBanned(testUser, testChannel, LocalDateTime.now())).thenReturn(true);

        // Act
        boolean result = moderationService.isUserBanned(testUser, testChannel);

        // Assert
        assertTrue(result);
    }

    @Test
    void isUserMuted_ActiveMute_ReturnsTrue() {
        // Arrange
        when(muteRepository.isUserMuted(testUser, testChannel, LocalDateTime.now())).thenReturn(true);

        // Act
        boolean result = moderationService.isUserMuted(testUser, testChannel);

        // Assert
        assertTrue(result);
    }

    @Test
    void unbanUser_Success() {
        // Arrange
        ChannelBan ban = new ChannelBan();
        ban.setId(1L);
        ban.setUser(testUser);
        ban.setChannel(testChannel);
        ban.setActive(true);

        when(banRepository.findById(1L)).thenReturn(Optional.of(ban));

        // Act
        moderationService.unbanUser(1L, adminUser, "Appeal accepted");

        // Assert
        verify(banRepository, times(1)).unbanUser(eq(1L), eq(adminUser), any(), eq("Appeal accepted"));
    }

    @Test
    void unmuteUser_Success() {
        // Arrange
        ChannelMute mute = new ChannelMute();
        mute.setId(1L);
        mute.setUser(testUser);
        mute.setChannel(testChannel);
        mute.setActive(true);

        when(muteRepository.findById(1L)).thenReturn(Optional.of(mute));

        // Act
        moderationService.unmuteUser(1L, adminUser);

        // Assert
        verify(muteRepository, times(1)).unmuteUser(eq(1L), eq(adminUser), any());
    }
}
