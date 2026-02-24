package com.lazycord.service;

import com.lazycord.model.Channel;
import com.lazycord.model.User;
import com.lazycord.repository.ChannelMemberRepository;
import com.lazycord.repository.ChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ChannelMemberRepository channelMemberRepository;

    @InjectMocks
    private ChannelService channelService;

    private User testUser;
    private Channel testChannel;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");

        testChannel = new Channel();
        testChannel.setId(UUID.randomUUID());
        testChannel.setName("test-channel");
        testChannel.setType(Channel.ChannelType.PUBLIC);
    }

    @Test
    void createChannel_Success() {
        // Arrange
        when(channelRepository.save(any(Channel.class))).thenReturn(testChannel);

        // Act
        Channel result = channelService.createChannel("test-channel", "Test Description", 
                Channel.ChannelType.PUBLIC, testUser);

        // Assert
        assertNotNull(result);
        assertEquals("test-channel", result.getName());
        verify(channelRepository, times(1)).save(any(Channel.class));
        verify(channelMemberRepository, times(1)).save(any());
    }

    @Test
    void joinChannel_Success() {
        // Arrange
        when(channelRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(channelMemberRepository.existsByChannelAndUser(testChannel, testUser)).thenReturn(false);
        when(channelMemberRepository.save(any())).thenReturn(null);

        // Act
        channelService.joinChannel(testChannel, testUser);

        // Assert
        verify(channelMemberRepository, times(1)).save(any());
    }

    @Test
    void joinChannel_AlreadyMember_ThrowsException() {
        // Arrange
        when(channelRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));
        when(channelMemberRepository.existsByChannelAndUser(testChannel, testUser)).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            channelService.joinChannel(testChannel, testUser);
        });
    }

    @Test
    void findById_Exists_ReturnsChannel() {
        // Arrange
        when(channelRepository.findById(testChannel.getId())).thenReturn(Optional.of(testChannel));

        // Act
        Optional<Channel> result = channelService.findById(testChannel.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testChannel.getName(), result.get().getName());
    }

    @Test
    void findById_NotExists_ReturnsEmpty() {
        // Arrange
        UUID randomId = UUID.randomUUID();
        when(channelRepository.findById(randomId)).thenReturn(Optional.empty());

        // Act
        Optional<Channel> result = channelService.findById(randomId);

        // Assert
        assertTrue(result.isEmpty());
    }
}
