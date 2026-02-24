package com.lazycord.repository;

import com.lazycord.model.Channel;
import com.lazycord.model.ChannelMute;
import com.lazycord.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelMuteRepository extends JpaRepository<ChannelMute, Long> {

    List<ChannelMute> findByChannelAndActiveTrue(Channel channel);

    Optional<ChannelMute> findByUserAndChannelAndActiveTrue(User user, Channel channel);

    boolean existsByUserAndChannelAndActiveTrue(User user, Channel channel);

    @Query("SELECT cm FROM ChannelMute cm WHERE cm.channel = :channel AND cm.active = true AND (cm.expiresAt IS NULL OR cm.expiresAt > :now)")
    List<ChannelMute> findActiveMutes(@Param("channel") Channel channel, @Param("now") LocalDateTime now);

    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END FROM ChannelMute cm WHERE cm.user = :user AND cm.channel = :channel AND cm.active = true AND (cm.expiresAt IS NULL OR cm.expiresAt > :now)")
    boolean isUserMuted(@Param("user") User user, @Param("channel") Channel channel, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE ChannelMute cm SET cm.active = false, cm.unmutedBy = :unmutedBy, cm.unmutedAt = :now WHERE cm.id = :muteId")
    void unmuteUser(@Param("muteId") Long muteId, @Param("unmutedBy") User unmutedBy, @Param("now") LocalDateTime now);
}
