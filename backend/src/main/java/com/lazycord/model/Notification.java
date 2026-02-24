package com.lazycord.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String message;

    @Column(name = "data_json", length = 2000)
    private String data;

    @Column(nullable = false)
    private boolean read = false;

    @Column
    private LocalDateTime readAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum NotificationType {
        MENTION,           // User was mentioned
        MESSAGE,           // New message in followed channel
        MISSION_COMPLETE,  // Mission completed
        LEVEL_UP,          // User leveled up
        SYSTEM             // System notification
    }
}
