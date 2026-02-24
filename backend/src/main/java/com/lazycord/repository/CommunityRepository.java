package com.lazycord.repository;

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
public interface CommunityRepository extends JpaRepository<Community, UUID> {

    Optional<Community> findByEmbedId(UUID embedId);

    Optional<Community> findBySlug(String slug);

    List<Community> findByIsPublicTrueAndActiveTrue();

    @Query("SELECT c FROM Community c WHERE c.owner = :user OR EXISTS (SELECT cm FROM CommunityMember cm WHERE cm.community = c AND cm.user = :user AND cm.active = true)")
    List<Community> findByUserMembership(@Param("user") User user);

    boolean existsBySlug(String slug);

    boolean existsByEmbedId(UUID embedId);

    Optional<Community> findByApiKey(String apiKey);
}
