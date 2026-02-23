package com.lazycord.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_bans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(length = 1000)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by", nullable = false)
    private User bannedBy;

    @CreationTimestamp
    private LocalDateTime bannedAt;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unbanned_by")
    private User unbannedBy;

    @Column
    private LocalDateTime unbannedAt;

    @Column(length = 1000)
    private String unbanReason;

    public boolean isExpired() {
        if (expiresAt == null) {
            return false; // Permanent ban
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
