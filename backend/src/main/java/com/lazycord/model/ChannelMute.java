package com.lazycord.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel_mutes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelMute {

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
    @JoinColumn(name = "muted_by", nullable = false)
    private User mutedBy;

    @CreationTimestamp
    private LocalDateTime mutedAt;

    @Column
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unmuted_by")
    private User unmutedBy;

    @Column
    private LocalDateTime unmutedAt;

    public boolean isExpired() {
        if (expiresAt == null) {
            return false; // Permanent mute
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
