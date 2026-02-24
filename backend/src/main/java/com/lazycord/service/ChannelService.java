package com.lazycord.service;

import com.lazycord.model.Channel;
import com.lazycord.model.ChannelMember;
import com.lazycord.model.User;
import com.lazycord.repository.ChannelMemberRepository;
import com.lazycord.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;

    @Transactional
    public Channel createChannel(String name, String description, Channel.ChannelType type, 
                                  User creator, Community community) {
        Channel channel = new Channel();
        channel.setName(name);
        channel.setDescription(description);
        channel.setType(type);
        channel.setCreatedBy(creator);
        channel.setCommunity(community);  // Set community
        
        Channel savedChannel = channelRepository.save(channel);
        
        // Add creator as owner
        ChannelMember member = new ChannelMember();
        member.setChannel(savedChannel);
        member.setUser(creator);
        member.setRole(ChannelMember.MemberRole.OWNER);
        channelMemberRepository.save(member);
        
        return savedChannel;
    }

    @Transactional(readOnly = true)
    public Optional<Channel> findById(UUID id) {
        return channelRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Channel> findAllPublicChannels(Community community) {
        return channelRepository.findByTypeAndCommunity(Channel.ChannelType.PUBLIC, community);
    }

    @Transactional(readOnly = true)
    public List<Channel> findUserChannels(User user, Community community) {
        return channelRepository.findByMemberAndCommunity(user, community);
    }

    @Transactional
    public void joinChannel(Channel channel, User user) {
        if (channelMemberRepository.existsByChannelAndUser(channel, user)) {
            throw new RuntimeException("User is already a member of this channel");
        }

        ChannelMember member = new ChannelMember();
        member.setChannel(channel);
        member.setUser(user);
        member.setRole(ChannelMember.MemberRole.MEMBER);

        channelMemberRepository.save(member);
    }

    @Transactional
    public void leaveChannel(Channel channel, User user) {
        ChannelMember member = channelMemberRepository.findByChannelAndUser(channel, user)
                .orElseThrow(() -> new RuntimeException("User is not a member of this channel"));

        // Prevent owner from leaving without transferring ownership
        if (member.getRole() == ChannelMember.MemberRole.OWNER) {
            long ownerCount = channelMemberRepository.findByChannel(channel).stream()
                    .filter(m -> m.getRole() == ChannelMember.MemberRole.OWNER)
                    .count();
            if (ownerCount <= 1) {
                throw new RuntimeException("Cannot leave channel as the only owner");
            }
        }

        channelMemberRepository.delete(member);
    }

    @Transactional
    public Channel createDirectMessage(User user1, User user2, Community community) {
        // Check if DM already exists
        Optional<Channel> existingDm = channelRepository.findDirectChannelBetweenUsersInCommunity(user1, user2, community);
        if (existingDm.isPresent()) {
            return existingDm.get();
        }

        Channel channel = new Channel();
        channel.setName(user1.getUsername() + " - " + user2.getUsername());
        channel.setType(Channel.ChannelType.DIRECT);
        channel.setCreatedBy(user1);
        channel.setCommunity(community);  // Set community

        Channel savedChannel = channelRepository.save(channel);

        // Add both users as members
        ChannelMember member1 = new ChannelMember();
        member1.setChannel(savedChannel);
        member1.setUser(user1);
        member1.setRole(ChannelMember.MemberRole.MEMBER);
        channelMemberRepository.save(member1);

        ChannelMember member2 = new ChannelMember();
        member2.setChannel(savedChannel);
        member2.setUser(user2);
        member2.setRole(ChannelMember.MemberRole.MEMBER);
        channelMemberRepository.save(member2);

        return savedChannel;
    }
}
