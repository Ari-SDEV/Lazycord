package com.lazycord.dto;

import com.lazycord.model.Community;

import java.time.LocalDateTime;
import java.util.UUID;

public class CommunityDto {
    private UUID id;
    private UUID embedId;
    private String name;
    private String description;
    private boolean isPublic;
    private boolean active;
    private UserDto owner;
    private LocalDateTime createdAt;

    public CommunityDto() {}

    public CommunityDto(Community community) {
        this.id = community.getId();
        this.embedId = community.getEmbedId();
        this.name = community.getName();
        this.description = community.getDescription();
        this.isPublic = community.isPublic();
        this.active = community.isActive();
        this.owner = community.getOwner() != null ? new UserDto(community.getOwner()) : null;
        this.createdAt = community.getCreatedAt();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmbedId() {
        return embedId;
    }

    public void setEmbedId(UUID embedId) {
        this.embedId = embedId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UserDto getOwner() {
        return owner;
    }

    public void setOwner(UserDto owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
