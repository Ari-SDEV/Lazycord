package com.lazycord.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lazycord.model.Notification;
import com.lazycord.model.User;
import com.lazycord.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Notification createNotification(User user, Notification.NotificationType type, 
                                          String title, String message, Map<String, Object> data) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        
        try {
            if (data != null) {
                notification.setData(objectMapper.writeValueAsString(data));
            }
        } catch (Exception e) {
            log.error("Failed to serialize notification data", e);
        }

        Notification saved = notificationRepository.save(notification);
        
        // Send real-time notification via WebSocket
        sendRealtimeNotification(user, saved);
        
        log.info("Created notification for user {}: {}", user.getUsername(), title);
        return saved;
    }

    @Transactional
    public void notifyMention(User mentionedUser, User mentioningUser, String channelName, String messagePreview) {
        Map<String, Object> data = new HashMap<>();
        data.put("mentioningUser", mentioningUser.getUsername());
        data.put("channelName", channelName);
        data.put("messagePreview", messagePreview);
        
        createNotification(
            mentionedUser,
            Notification.NotificationType.MENTION,
            "New Mention",
            mentioningUser.getUsername() + " mentioned you in #" + channelName,
            data
        );
    }

    @Transactional
    public void notifyNewMessage(User user, String channelName, String senderName) {
        Map<String, Object> data = new HashMap<>();
        data.put("channelName", channelName);
        data.put("senderName", senderName);
        
        createNotification(
            user,
            Notification.NotificationType.MESSAGE,
            "New Message",
            senderName + " sent a message in #" + channelName,
            data
        );
    }

    @Transactional
    public void notifyMissionComplete(User user, String missionTitle, int xpReward, int pointsReward) {
        Map<String, Object> data = new HashMap<>();
        data.put("missionTitle", missionTitle);
        data.put("xpReward", xpReward);
        data.put("pointsReward", pointsReward);
        
        createNotification(
            user,
            Notification.NotificationType.MISSION_COMPLETE,
            "Mission Complete!",
            "You completed '" + missionTitle + "' and earned " + xpReward + " XP!",
            data
        );
    }

    @Transactional
    public void notifyLevelUp(User user, int newLevel, String newRank) {
        Map<String, Object> data = new HashMap<>();
        data.put("newLevel", newLevel);
        data.put("newRank", newRank);
        
        createNotification(
            user,
            Notification.NotificationType.LEVEL_UP,
            "Level Up!",
            "Congratulations! You reached level " + newLevel + " (" + newRank + ")!",
            data
        );
    }

    private void sendRealtimeNotification(User user, Notification notification) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", notification.getId());
            payload.put("type", notification.getType());
            payload.put("title", notification.getTitle());
            payload.put("message", notification.getMessage());
            payload.put("data", notification.getData());
            payload.put("createdAt", notification.getCreatedAt());
            payload.put("read", notification.isRead());
            
            messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications",
                payload
            );
            
            // Also send unread count
            long unreadCount = getUnreadCount(user);
            messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/notifications/count",
                Map.of("count", unreadCount)
            );
            
        } catch (Exception e) {
            log.error("Failed to send realtime notification", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndReadFalseOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndReadFalse(user);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user);
        
        // Send updated count
        messagingTemplate.convertAndSendToUser(
            user.getUsername(),
            "/queue/notifications/count",
            Map.of("count", 0)
        );
    }

    @Transactional
    public void deleteNotification(UUID notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to delete this notification");
        }
        
        notificationRepository.delete(notification);
    }
}
