package com.lazycord.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "missions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false)
    private int xpReward;

    @Column(nullable = false)
    private int pointsReward;

    @Column(nullable = false)
    private int requiredCount;

    @Column
    private String spelExpression;

    @Column
    private LocalDateTime startDate;

    @Column
    private LocalDateTime endDate;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum MissionType {
        DAILY,
        WEEKLY,
        MONTHLY,
        ONE_TIME,
        ACHIEVEMENT
    }

    public enum Difficulty {
        EASY(1.0),
        MEDIUM(1.5),
        HARD(2.0),
        EXPERT(3.0);

        private final double multiplier;

        Difficulty(double multiplier) {
            this.multiplier = multiplier;
        }

        public double getMultiplier() {
            return multiplier;
        }
    }
}
