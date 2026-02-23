package com.lazycord.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissionDto {
    private UUID id;
    private String title;
    private String description;
    private String type;
    private String difficulty;
    private int xpReward;
    private int pointsReward;
    private int requiredCount;
    private int currentCount;
    private boolean completed;
    private boolean rewarded;
    private LocalDateTime endDate;
}
