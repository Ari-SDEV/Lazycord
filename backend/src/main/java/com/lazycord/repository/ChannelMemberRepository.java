package com.lazycord.repository;

import com.lazycord.model.Channel;
import com.lazycord.model.ChannelMember;
import com.lazycord.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, Long> {

    List<ChannelMember> findByChannel(Channel channel);

    Optional<ChannelMember> findByChannelAndUser(Channel channel, User user);

    List<ChannelMember> findByUser(User user);

    boolean existsByChannelAndUser(Channel channel, User user);

    void deleteByChannelAndUser(Channel channel, User user);
}
