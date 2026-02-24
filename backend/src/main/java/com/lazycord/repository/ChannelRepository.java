package com.lazycord.repository;

import com.lazycord.model.Channel;
import com.lazycord.model.Channel.ChannelType;
import com.lazycord.model.Community;
import com.lazycord.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    List<Channel> findByType(ChannelType type);

    @Query("SELECT c FROM Channel c JOIN c.members cm WHERE cm.user = :user")
    List<Channel> findByMember(@Param("user") User user);

    @Query("SELECT c FROM Channel c WHERE c.type = 'DIRECT' AND " +
           "(SELECT COUNT(cm) FROM ChannelMember cm WHERE cm.channel = c AND cm.user IN (:user1, :user2)) = 2")
    Optional<Channel> findDirectChannelBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    List<Channel> findByNameContainingIgnoreCase(String name);

    // Community-filtered queries
    List<Channel> findByTypeAndCommunity(ChannelType type, Community community);

    @Query("SELECT c FROM Channel c JOIN c.members cm WHERE cm.user = :user AND c.community = :community")
    List<Channel> findByMemberAndCommunity(@Param("user") User user, @Param("community") Community community);
}
