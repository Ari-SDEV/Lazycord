package com.lazycord.service;

import com.lazycord.model.Community;
import com.lazycord.model.CommunityMember;
import com.lazycord.model.User;
import com.lazycord.repository.CommunityMemberRepository;
import com.lazycord.repository.CommunityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CommunityService {

    private static final Logger log = LoggerFactory.getLogger(CommunityService.class);

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;

    public CommunityService(CommunityRepository communityRepository, CommunityMemberRepository communityMemberRepository) {
        this.communityRepository = communityRepository;
        this.communityMemberRepository = communityMemberRepository;
    }

    @Transactional
    public Community createCommunity(String name, String description, User owner, boolean isPublic) {
        // Generate unique embedId
        UUID embedId = UUID.randomUUID();
        while (communityRepository.existsByEmbedId(embedId)) {
            embedId = UUID.randomUUID();
        }

        // Generate API key
        String apiKey = generateApiKey();

        Community community = new Community();
        community.setName(name);
        community.setDescription(description);
        community.setOwner(owner);
        community.setEmbedId(embedId);
        community.setApiKey(apiKey);
        community.setPublic(isPublic);
        community.setActive(true);

        Community savedCommunity = communityRepository.save(community);

        // Add owner as OWNER member
        CommunityMember member = new CommunityMember();
        member.setUser(owner);
        member.setCommunity(savedCommunity);
        member.setRole(CommunityMember.CommunityRole.OWNER);
        member.setActive(true);
        communityMemberRepository.save(member);

        log.info("Community created: {} by {}", name, owner.getUsername());
        return savedCommunity;
    }

    @Transactional(readOnly = true)
    public Optional<Community> findByEmbedId(UUID embedId) {
        return communityRepository.findByEmbedId(embedId);
    }

    @Transactional(readOnly = true)
    public Optional<Community> findById(UUID id) {
        return communityRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Community> getUserCommunities(User user) {
        return communityRepository.findByUserMembership(user);
    }

    @Transactional(readOnly = true)
    public Optional<CommunityMember> findMembership(User user, Community community) {
        return communityMemberRepository.findByUserAndCommunity(user, community);
    }

    @Transactional(readOnly = true)
    public List<Community> getPublicCommunities() {
        return communityRepository.findByIsPublicTrueAndActiveTrue();
    }

    @Transactional
    public void joinCommunity(User user, Community community) {
        if (communityMemberRepository.existsByUserAndCommunityAndActiveTrue(user, community)) {
            throw new RuntimeException("Already a member of this community");
        }

        CommunityMember member = new CommunityMember();
        member.setUser(user);
        member.setCommunity(community);
        member.setRole(CommunityMember.CommunityRole.MEMBER);
        member.setActive(true);
        communityMemberRepository.save(member);

        log.info("User {} joined community {}", user.getUsername(), community.getName());
    }

    @Transactional
    public void leaveCommunity(User user, Community community) {
        CommunityMember member = communityMemberRepository.findByUserAndCommunity(user, community)
                .orElseThrow(() -> new RuntimeException("Not a member of this community"));

        // Owner cannot leave (must transfer ownership first)
        if (member.getRole() == CommunityMember.CommunityRole.OWNER) {
            long ownerCount = communityMemberRepository.findByCommunityAndActiveTrue(community).stream()
                    .filter(m -> m.getRole() == CommunityMember.CommunityRole.OWNER)
                    .count();
            if (ownerCount <= 1) {
                throw new RuntimeException("Owner cannot leave. Transfer ownership first.");
            }
        }

        member.setActive(false);
        communityMemberRepository.save(member);

        log.info("User {} left community {}", user.getUsername(), community.getName());
    }

    @Transactional
    public void regenerateApiKey(Community community) {
        String newApiKey = generateApiKey();
        community.setApiKey(newApiKey);
        communityRepository.save(community);
        log.info("API key regenerated for community {}", community.getName());
    }

    @Transactional(readOnly = true)
    public boolean isUserInCommunity(User user, Community community) {
        return communityMemberRepository.existsByUserAndCommunityAndActiveTrue(user, community);
    }

    @Transactional(readOnly = true)
    public boolean isUserAdminOrOwner(User user, Community community) {
        Optional<CommunityMember> member = communityMemberRepository.findByUserAndCommunity(user, community);
        return member.isPresent() && 
               (member.get().getRole() == CommunityMember.CommunityRole.ADMIN || 
                member.get().getRole() == CommunityMember.CommunityRole.OWNER);
    }

    @Transactional(readOnly = true)
    public long getMemberCount(Community community) {
        return communityMemberRepository.countByCommunityAndActiveTrue(community);
    }

    private String generateApiKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            keyGen.init(256, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            // Fallback
            byte[] bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            return Base64.getEncoder().encodeToString(bytes);
        }
    }
}
