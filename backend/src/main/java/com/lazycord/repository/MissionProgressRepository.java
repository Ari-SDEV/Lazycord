package com.lazycord.repository;

import com.lazycord.model.Mission;
import com.lazycord.model.MissionProgress;
import com.lazycord.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MissionProgressRepository extends JpaRepository<MissionProgress, Long> {

    List<MissionProgress> findByUser(User user);

    Optional<MissionProgress> findByUserAndMission(User user, Mission mission);

    List<MissionProgress> findByUserAndCompletedFalse(User user);

    List<MissionProgress> findByUserAndCompletedTrue(User user);

    @Query("SELECT mp FROM MissionProgress mp WHERE mp.user = :user AND mp.completed = true AND mp.rewarded = false")
    List<MissionProgress> findCompletedUnrewardedByUser(@Param("user") User user);

    @Query("SELECT mp FROM MissionProgress mp WHERE mp.completed = true AND mp.completedAt >= :since")
    List<MissionProgress> findRecentlyCompleted(@Param("since") LocalDateTime since);
}
