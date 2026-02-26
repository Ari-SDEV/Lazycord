package com.lazycord.dto;

import com.lazycord.model.Community;
import com.lazycord.model.CommunityMember;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Community with the current user's role in that community.
 */
public class CommunityWithRoleDto {
    private UUID id;
    private UUID embedId;
    private String name;
    private String description;
    private boolean isPublic;
    private boolean active;
    private UserDto owner;
    private LocalDateTime createdAt;
    
    // User's role in this community
    private CommunityRole userRole;
    private boolean isMember;
    
    public enum CommunityRole {
        OWNER, ADMIN, MODERATOR, MEMBER, NONE
    }
    
    public CommunityWithRoleDto() {}
    
    public CommunityWithRoleDto(Community community, CommunityMember member) {
        this.id = community.getId();
        this.embedId = community.getEmbedId();
        this.name = community.getName();
        this.description = community.getDescription();
        this.isPublic = community.isPublic();
        this.active = community.isActive();
        this.owner = community.getOwner() != null ? new UserDto(community.getOwner()) : null;
        this.createdAt = community.getCreatedAt();
        
        if (member != null && member.isActive()) {
            this.userRole = CommunityRole.valueOf(member.getRole().name());
            this.isMember = true;
        } else {
            this.userRole = CommunityRole.NONE;
            this.isMember = false;
        }
    }
    
    public CommunityWithRoleDto(Community community) {
        this(community, null);
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getEmbedId() { return embedId; }
    public void setEmbedId(UUID embedId) { this.embedId = embedId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public UserDto getOwner() { return owner; }
    public void setOwner(UserDto owner) { this.owner = owner; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public CommunityRole getUserRole() { return userRole; }
    public void setUserRole(CommunityRole userRole) { this.userRole = userRole; }
    
    public boolean isMember() { return isMember; }
    public void setMember(boolean isMember) { this.isMember = isMember; }
    
    /**
     * Check if user has at least the specified role level
     */
    public boolean hasRole(CommunityRole requiredRole) {
        if (userRole == null || userRole == CommunityRole.NONE) {
            return false;
        }
        
        int userLevel = getRoleLevel(userRole);
        int requiredLevel = getRoleLevel(requiredRole);
        
        return userLevel >= requiredLevel;
    }
    
    private int getRoleLevel(CommunityRole role) {
        return switch (role) {
            case OWNER -> 4;
            case ADMIN -> 3;
            case MODERATOR -> 2;
            case MEMBER -> 1;
            case NONE -> 0;
        };
    }
    
    public boolean isOwner() {
        return userRole == CommunityRole.OWNER;
    }
    
    public boolean isAdmin() {
        return userRole == CommunityRole.OWNER || userRole == CommunityRole.ADMIN;
    }
    
    public boolean isModerator() {
        return userRole == CommunityRole.OWNER || userRole == CommunityRole.ADMIN || userRole == CommunityRole.MODERATOR;
    }
}
