package com.lazycord.controller;

import com.lazycord.dto.CommunityDto;
import com.lazycord.model.Community;
import com.lazycord.model.User;
import com.lazycord.service.CommunityService;
import com.lazycord.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/communities")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:1420", "tauri://localhost"})
public class CommunityController {

    private static final Logger log = LoggerFactory.getLogger(CommunityController.class);

    private final CommunityService communityService;
    private final UserService userService;

    public CommunityController(CommunityService communityService, UserService userService) {
        this.communityService = communityService;
        this.userService = userService;
    }

    @GetMapping("/my")
    public ResponseEntity<List<CommunityDto>> getMyCommunities(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Optional<User> userOpt = userService.findByUsername(authentication.getName());
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            User user = userOpt.get();
            List<Community> communities = communityService.getUserCommunities(user);
            
            List<CommunityDto> dtos = communities.stream()
                    .map(CommunityDto::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error fetching user communities: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/public")
    public ResponseEntity<List<CommunityDto>> getPublicCommunities() {
        try {
            List<Community> communities = communityService.getPublicCommunities();
            List<CommunityDto> dtos = communities.stream()
                    .map(CommunityDto::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            log.error("Error fetching public communities: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createCommunity(
            @RequestBody CreateCommunityRequest request,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Optional<User> userOpt = userService.findByUsername(authentication.getName());
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            Community community = communityService.createCommunity(
                    request.getName(),
                    request.getDescription(),
                    user,
                    request.isPublic()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(new CommunityDto(community));
        } catch (Exception e) {
            log.error("Error creating community: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{communityId}/join")
    public ResponseEntity<?> joinCommunity(
            @PathVariable String communityId,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Optional<User> userOpt = userService.findByUsername(authentication.getName());
            Optional<Community> communityOpt = communityService.findById(java.util.UUID.fromString(communityId));

            if (!userOpt.isPresent() || !communityOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User or community not found"));
            }

            communityService.joinCommunity(userOpt.get(), communityOpt.get());
            return ResponseEntity.ok(Map.of("message", "Joined community successfully"));
        } catch (Exception e) {
            log.error("Error joining community: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    public static class CreateCommunityRequest {
        private String name;
        private String description;
        private boolean isPublic = true;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isPublic() { return isPublic; }
        public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    }
}
