package com.lazycord.service;

import com.lazycord.model.*;
import com.lazycord.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {

    private final ChannelBanRepository banRepository;
    private final ChannelMuteRepository muteRepository;
    private final ReportRepository reportRepository;
    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;
    private final ChannelMemberRepository channelMemberRepository;

    // Ban operations
    @Transactional
    public void banUser(User user, Channel channel, String reason, User bannedBy, Duration duration) {
        // Check if already banned
        if (banRepository.isUserBanned(user, channel, LocalDateTime.now())) {
            throw new RuntimeException("User is already banned from this channel");
        }

        ChannelBan ban = new ChannelBan();
        ban.setUser(user);
        ban.setChannel(channel);
        ban.setReason(reason);
        ban.setBannedBy(bannedBy);
        ban.setActive(true);

        if (duration != null) {
            ban.setExpiresAt(LocalDateTime.now().plus(duration.getDuration(), duration.getUnit()));
        }

        banRepository.save(ban);

        // Remove user from channel
        channelMemberRepository.findByChannelAndUser(channel, user).ifPresent(cm -> {
            channelMemberRepository.delete(cm);
        });

        log.info("User {} banned from channel {} by {}", user.getUsername(), channel.getName(), bannedBy.getUsername());
    }

    @Transactional
    public void unbanUser(Long banId, User unbannedBy, String reason) {
        ChannelBan ban = banRepository.findById(banId)
                .orElseThrow(() -> new RuntimeException("Ban not found"));

        if (!ban.isActive()) {
            throw new RuntimeException("User is not banned");
        }

        banRepository.unbanUser(banId, unbannedBy, LocalDateTime.now(), reason);

        log.info("User {} unbanned from channel {} by {}",
                ban.getUser().getUsername(), ban.getChannel().getName(), unbannedBy.getUsername());
    }

    @Transactional(readOnly = true)
    public boolean isUserBanned(User user, Channel channel) {
        return banRepository.isUserBanned(user, channel, LocalDateTime.now());
    }

    // Mute operations
    @Transactional
    public void muteUser(User user, Channel channel, String reason, User mutedBy, Duration duration) {
        // Check if already muted
        if (muteRepository.isUserMuted(user, channel, LocalDateTime.now())) {
            throw new RuntimeException("User is already muted in this channel");
        }

        ChannelMute mute = new ChannelMute();
        mute.setUser(user);
        mute.setChannel(channel);
        mute.setReason(reason);
        mute.setMutedBy(mutedBy);
        mute.setActive(true);

        if (duration != null) {
            mute.setExpiresAt(LocalDateTime.now().plus(duration.getDuration(), duration.getUnit()));
        }

        muteRepository.save(mute);

        log.info("User {} muted in channel {} by {}", user.getUsername(), channel.getName(), mutedBy.getUsername());
    }

    @Transactional
    public void unmuteUser(Long muteId, User unmutedBy) {
        ChannelMute mute = muteRepository.findById(muteId)
                .orElseThrow(() -> new RuntimeException("Mute not found"));

        if (!mute.isActive()) {
            throw new RuntimeException("User is not muted");
        }

        muteRepository.unmuteUser(muteId, unmutedBy, LocalDateTime.now());

        log.info("User {} unmuted in channel {} by {}",
                mute.getUser().getUsername(), mute.getChannel().getName(), unmutedBy.getUsername());
    }

    @Transactional(readOnly = true)
    public boolean isUserMuted(User user, Channel channel) {
        return muteRepository.isUserMuted(user, channel, LocalDateTime.now());
    }

    // Report operations
    @Transactional
    public Report createReport(User reporter, User reportedUser, Channel channel, Message message,
                           Report.ReportReason reason, String details) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setChannel(channel);
        report.setMessage(message);
        report.setReason(reason);
        report.setDetails(details);
        report.setStatus(Report.ReportStatus.PENDING);

        return reportRepository.save(report);
    }

    @Transactional
    public void resolveReport(UUID reportId, User resolvedBy, String resolution, boolean actionTaken) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(Report.ReportStatus.RESOLVED);
        report.setResolvedBy(resolvedBy);
        report.setResolvedAt(LocalDateTime.now());
        report.setResolution(resolution);

        reportRepository.save(report);

        log.info("Report {} resolved by {} with action taken: {}", reportId, resolvedBy.getUsername(), actionTaken);
    }

    // Getters
    @Transactional(readOnly = true)
    public List<ChannelBan> getChannelBans(UUID channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
        return banRepository.findActiveBans(channel, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<ChannelMute> getChannelMutes(UUID channelId) {
        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
        return muteRepository.findActiveMutes(channel, LocalDateTime.now());
    }

    // Duration helper class
    public static class Duration {
        private final long duration;
        private final ChronoUnit unit;

        public Duration(long duration, ChronoUnit unit) {
            this.duration = duration;
            this.unit = unit;
        }

        public static Duration minutes(long minutes) {
            return new Duration(minutes, ChronoUnit.MINUTES);
        }

        public static Duration hours(long hours) {
            return new Duration(hours, ChronoUnit.HOURS);
        }

        public static Duration days(long days) {
            return new Duration(days, ChronoUnit.DAYS);
        }

        public static Duration weeks(long weeks) {
            return new Duration(weeks * 7, ChronoUnit.DAYS);
        }

        public static Duration permanent() {
            return null;
        }

        public long getDuration() { return duration; }
        public ChronoUnit getUnit() { return unit; }
    }
}
