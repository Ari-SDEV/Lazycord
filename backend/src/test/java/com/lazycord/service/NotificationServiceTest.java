package com.lazycord.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lazycord.model.Notification;
import com.lazycord.model.User;
import com.lazycord.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("testuser");
    }

    @Test
    void createNotification_Success() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            return notification;
        });

        // Act
        Notification result = notificationService.createNotification(
                testUser,
                Notification.NotificationType.MENTION,
                "New Mention",
                "You were mentioned",
                null
        );

        // Assert
        assertNotNull(result);
        assertEquals(Notification.NotificationType.MENTION, result.getType());
        assertEquals("New Mention", result.getTitle());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void notifyMention_Success() {
        // Arrange
        User mentioningUser = new User();
        mentioningUser.setUsername("otheruser");

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            return notification;
        });

        // Act
        notificationService.notifyMention(testUser, mentioningUser, "general", "Hello @testuser");

        // Assert
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void notifyMissionComplete_Success() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            return notification;
        });

        // Act
        notificationService.notifyMissionComplete(testUser, "Daily Login", 100, 50);

        // Assert
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void notifyLevelUp_Success() {
        // Arrange
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            return notification;
        });

        // Act
        notificationService.notifyLevelUp(testUser, 5, "GOLD");

        // Assert
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void getUnreadCount_ReturnsCount() {
        // Arrange
        when(notificationRepository.countByUserAndReadFalse(testUser)).thenReturn(5L);

        // Act
        long count = notificationService.getUnreadCount(testUser);

        // Assert
        assertEquals(5L, count);
    }

    @Test
    void getUserNotifications_ReturnsPage() {
        // Arrange
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUser(testUser);
        notification.setTitle("Test");

        Page<Notification> page = new PageImpl<>(Collections.singletonList(notification));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(eq(testUser), any()))
                .thenReturn(page);

        // Act
        Page<Notification> result = notificationService.getUserNotifications(testUser,
                PageRequest.of(0, 20));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void markAllAsRead_Success() {
        // Act
        notificationService.markAllAsRead(testUser);

        // Assert
        verify(notificationRepository, times(1)).markAllAsRead(testUser);
    }
}
