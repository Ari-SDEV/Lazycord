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

    List<Rank> findByActiveTrueOrderBySortOrderAsc();

    Optional<Rank> findByNameAndActiveTrue(String name);

    @Query("SELECT r FROM Rank r WHERE r.active = true AND r.minLevel <= :level AND r.maxLevel >= :level ORDER BY r.sortOrder")
    Optional<Rank> findByLevel(@Param("level") int level);

    @Query("SELECT r FROM Rank r WHERE r.active = true AND r.minLevel <= :level ORDER BY r.sortOrder DESC")
    Optional<Rank> findHighestRankForLevel(@Param("level") int level);

    boolean existsByName(String name);

    boolean existsByMinLevelLessThanEqualAndMaxLevelGreaterThanEqual(int minLevel, int maxLevel);
}
