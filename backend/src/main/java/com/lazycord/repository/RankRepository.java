package com.lazycord.repository;

import com.lazycord.model.Rank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RankRepository extends JpaRepository<Rank, UUID> {

    List<Rank> findByCommunityAndActiveTrueOrderBySortOrderAsc(Community community);

    Optional<Rank> findByCommunityAndNameAndActiveTrue(Community community, String name);

    @Query("SELECT r FROM Rank r WHERE r.community = :community AND r.active = true AND r.minLevel <= :level AND r.maxLevel >= :level ORDER BY r.sortOrder")
    Optional<Rank> findByCommunityAndLevel(@Param("community") Community community, @Param("level") int level);

    @Query("SELECT r FROM Rank r WHERE r.community = :community AND r.active = true AND r.minLevel <= :level ORDER BY r.sortOrder DESC")
    Optional<Rank> findHighestRankForLevelInCommunity(@Param("community") Community community, @Param("level") int level);

    // Legacy methods
    List<Rank> findByActiveTrueOrderBySortOrderAsc();
}
