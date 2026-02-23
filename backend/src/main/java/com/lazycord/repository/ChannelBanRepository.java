package com.lazycord.repository;

import com.lazycord.model.Channel;
import com.lazycord.model.ChannelBan;
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
public interface ChannelBanRepository extends JpaRepository<ChannelBan, Long> {

    List<ChannelBan> findByChannelAndActiveTrue(Channel channel);

    Optional<ChannelBan> findByUserAndChannelAndActiveTrue(User user, Channel channel);

    boolean existsByUserAndChannelAndActiveTrue(User user, Channel channel);

    @Query("SELECT cb FROM ChannelBan cb WHERE cb.channel = :channel AND cb.active = true AND (cb.expiresAt IS NULL OR cb.expiresAt > :now)")
    List<ChannelBan> findActiveBans(@Param("channel") Channel channel, @Param("now") LocalDateTime now);

    @Query("SELECT CASE WHEN COUNT(cb) > 0 THEN true ELSE false END FROM ChannelBan cb WHERE cb.user = :user AND cb.channel = :channel AND cb.active = true AND (cb.expiresAt IS NULL OR cb.expiresAt > :now)")
    boolean isUserBanned(@Param("user") User user, @Param("channel") Channel channel, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE ChannelBan cb SET cb.active = false, cb.unbannedBy = :unbannedBy, cb.unbannedAt = :now, cb.unbanReason = :reason WHERE cb.id = :banId")
    void unbanUser(@Param("banId") Long banId, @Param("unbannedBy") User unbannedBy, @Param("now") LocalDateTime now, @Param("reason") String reason);
}
