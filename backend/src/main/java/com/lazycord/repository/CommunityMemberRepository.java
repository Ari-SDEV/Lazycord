package com.lazycord.repository;

import com.lazycord.model.Community;
import com.lazycord.model.CommunityMember;
import com.lazycord.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, Long> {

    Optional<CommunityMember> findByUserAndCommunity(User user, Community community);

    List<CommunityMember> findByCommunityAndActiveTrue(Community community);

    List<CommunityMember> findByUserAndActiveTrue(User user);

    boolean existsByUserAndCommunityAndActiveTrue(User user, Community community);

    long countByCommunityAndActiveTrue(Community community);
}
