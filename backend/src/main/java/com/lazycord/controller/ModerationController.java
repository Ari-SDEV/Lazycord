package com.lazycord.controller;

import com.lazycord.model.*;
import com.lazycord.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:1420", "tauri://localhost"})
public class ModerationController {

    private final ModerationService moderationService;
    private final ChannelService channelService;
    private final UserService userService;
    private final ReportService reportService;

    // Ban endpoints
    @PostMapping("/channels/{channelId}/ban")
    public ResponseEntity<Void> banUser(
            @PathVariable UUID channelId,
            @RequestBody BanRequest request,
            Authentication authentication) {
        
        Channel channel = channelService.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
        User bannedBy = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User userToBan = userService.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User to ban not found"));
        
        ModerationService.Duration duration = parseDuration(request.getDuration());
        moderationService.banUser(userToBan, channel, request.getReason(), bannedBy, duration);
        
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/channels/{channelId}/ban/{userId}")
    public ResponseEntity<Void> unbanUser(
            @PathVariable UUID channelId,
            @PathVariable UUID userId,
            @RequestBody UnbanRequest request,
            Authentication authentication) {
        
        User unbannedBy = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Find the active ban
        ChannelBan ban = moderationService.getChannelBans(channelId).stream()
                .filter(b -> b.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Active ban not found"));
        
        moderationService.unbanUser(ban.getId(), unbannedBy, request.getReason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/channels/{channelId}/bans")
    public ResponseEntity<List<BanResponse>> getChannelBans(@PathVariable UUID channelId) {
        List<ChannelBan> bans = moderationService.getChannelBans(channelId);
        List<BanResponse> response = bans.stream()
                .map(b -> new BanResponse(
                        b.getId(),
                        b.getUser().getId(),
                        b.getUser().getUsername(),
                        b.getReason(),
                        b.getBannedBy().getUsername(),
                        b.getBannedAt(),
                        b.getExpiresAt(),
                        b.isExpired()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // Mute endpoints
    @PostMapping("/channels/{channelId}/mute")
    public ResponseEntity<Void> muteUser(
            @PathVariable UUID channelId,
            @RequestBody MuteRequest request,
            Authentication authentication) {
        
        Channel channel = channelService.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
        User mutedBy = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User userToMute = userService.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User to mute not found"));
        
        ModerationService.Duration duration = parseDuration(request.getDuration());
        moderationService.muteUser(userToMute, channel, request.getReason(), mutedBy, duration);
        
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/channels/{channelId}/mute/{userId}")
    public ResponseEntity<Void> unmuteUser(
            @PathVariable UUID channelId,
            @PathVariable UUID userId,
            Authentication authentication) {
        
        User unmutedBy = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ChannelMute mute = moderationService.getChannelMutes(channelId).stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Active mute not found"));
        
        moderationService.unmuteUser(mute.getId(), unmutedBy);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/channels/{channelId}/mutes")
    public ResponseEntity<List<MuteResponse>> getChannelMutes(@PathVariable UUID channelId) {
        List<ChannelMute> mutes = moderationService.getChannelMutes(channelId);
        List<MuteResponse> response = mutes.stream()
                .map(m -> new MuteResponse(
                        m.getId(),
                        m.getUser().getId(),
                        m.getUser().getUsername(),
                        m.getReason(),
                        m.getMutedBy().getUsername(),
                        m.getMutedAt(),
                        m.getExpiresAt(),
                        m.isExpired()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // Report endpoints
    @PostMapping("/reports")
    public ResponseEntity<ReportResponse> createReport(
            @RequestBody CreateReportRequest request,
            Authentication authentication) {
        
        User reporter = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        User reportedUser = userService.findById(request.getReportedUserId())
                .orElseThrow(() -> new RuntimeException("Reported user not found"));
        Channel channel = request.getChannelId() != null ? 
                channelService.findById(request.getChannelId()).orElse(null) : null;
        
        Report report = reportService.createReport(
                reporter, reportedUser, channel, null,
                request.getReason(), request.getDetails()
        );
        
        return ResponseEntity.ok(new ReportResponse(report));
    }

    // Helper methods
    private ModerationService.Duration parseDuration(String duration) {
        if (duration == null || duration.isEmpty() || "PERMANENT".equalsIgnoreCase(duration)) {
            return ModerationService.Duration.permanent();
        }
        
        String[] parts = duration.split(" ");
        long value = Long.parseLong(parts[0]);
        String unit = parts[1].toLowerCase();
        
        return switch (unit) {
            case "minutes", "minute", "min", "m" -> ModerationService.Duration.minutes(value);
            case "hours", "hour", "h" -> ModerationService.Duration.hours(value);
            case "days", "day", "d" -> ModerationService.Duration.days(value);
            case "weeks", "week", "w" -> ModerationService.Duration.weeks(value);
            default -> ModerationService.Duration.hours(value);
        };
    }

    // DTO classes
    @Data
    public static class BanRequest {
        private UUID userId;
        private String reason;
        private String duration; // e.g., "1 hour", "2 days", "PERMANENT"
    }

    @Data
    public static class UnbanRequest {
        private String reason;
    }

    @Data
    public static class MuteRequest {
        private UUID userId;
        private String reason;
        private String duration;
    }

    @Data
    public static class CreateReportRequest {
        private UUID reportedUserId;
        private UUID channelId;
        private Report.ReportReason reason;
        private String details;
    }

    @Data
    public static class BanResponse {
        private final Long banId;
        private final UUID userId;
        private final String username;
        private final String reason;
        private final String bannedBy;
        private final LocalDateTime bannedAt;
        private final LocalDateTime expiresAt;
        private final boolean expired;
    }

    @Data
    public static class MuteResponse {
        private final Long muteId;
        private final UUID userId;
        private final String username;
        private final String reason;
        private final String mutedBy;
        private final LocalDateTime mutedAt;
        private final LocalDateTime expiresAt;
        private final boolean expired;
    }

    @Data
    public static class ReportResponse {
        private final UUID id;
        private final String reporterUsername;
        private final String reportedUserUsername;
        private final Report.ReportReason reason;
        private final String details;
        private final Report.ReportStatus status;
        private final LocalDateTime createdAt;

        public ReportResponse(Report report) {
            this.id = report.getId();
            this.reporterUsername = report.getReporter().getUsername();
            this.reportedUserUsername = report.getReportedUser().getUsername();
            this.reason = report.getReason();
            this.details = report.getDetails();
            this.status = report.getStatus();
            this.createdAt = report.getCreatedAt();
        }
    }
}
