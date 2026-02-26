package com.lazycord.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_id", unique = true, nullable = false)
    private String keycloakId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    private int points = 0;

    @Column(nullable = false)
    private int xp = 0;

    @Column(nullable = false)
    private int level = 1;

    @Column(nullable = false)
    private String rank = "Newbie";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_active")
    private LocalDateTime lastActive;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActive = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastActive = LocalDateTime.now();
    }

    public User() {}

    public User(String keycloakId, String username, String email, String firstName, String lastName) {
        this.keycloakId = keycloakId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActive() {
        return lastActive;
    }

    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }

    public void addXp(int amount) {
        this.xp += amount;
        checkLevelUp();
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    private void checkLevelUp() {
        int newLevel = calculateLevelFromXp(this.xp);
        if (newLevel > this.level) {
            this.level = newLevel;
            this.rank = calculateRankFromLevel(newLevel);
        }
    }

    private int calculateLevelFromXp(int xp) {
        return Math.max(1, (int) Math.sqrt(xp / 100.0) + 1);
    }

    private String calculateRankFromLevel(int level) {
        if (level >= 50) return "Legend";
        if (level >= 40) return "Grandmaster";
        if (level >= 30) return "Master";
        if (level >= 20) return "Expert";
        if (level >= 10) return "Veteran";
        if (level >= 5) return "Regular";
        return "Newbie";
    }
}
