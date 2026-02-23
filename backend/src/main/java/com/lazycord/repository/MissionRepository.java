package com.lazycord.repository;

import com.lazycord.model.Mission;
import com.lazycord.model.Mission.MissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {

    List<Mission> findByActiveTrue();

    List<Mission> findByTypeAndActiveTrue(MissionType type);

    @Query("SELECT m FROM Mission m WHERE m.active = true AND " +
           "(m.startDate IS NULL OR m.startDate <= :now) AND " +
           "(m.endDate IS NULL OR m.endDate >= :now)")
    List<Mission> findAvailableMissions(@Param("now") LocalDateTime now);

    List<Mission> findByTypeAndDifficulty(MissionType type, Mission.Difficulty difficulty);
}
