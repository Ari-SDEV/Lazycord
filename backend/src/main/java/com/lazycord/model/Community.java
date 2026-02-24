package com.lazycord.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "communities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Public embed ID - nicht erratbar
    @Column(unique = true, nullable = false)
    private UUID embedId;

    // Anzeige-Name
    @Column(nullable = false)
    private String name;

    // Optional: Lesbarer Slug für URLs
    @Column(unique = true)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column
    private String avatarUrl;

    // Owner der Community
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // API Key - nur für Backend, hashed
    @Column(nullable = false)
    private String apiKey;

    // Erlaubte Domains für Embeds (JSON)
    @Column(columnDefinition = "TEXT")
    private String allowedDomains;

    @Column(nullable = false)
    private boolean isPublic = false;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Hilfsmethode für Embed-URL
    public String getEmbedUrl() {
        return "/embed/" + embedId.toString();
    }
}
