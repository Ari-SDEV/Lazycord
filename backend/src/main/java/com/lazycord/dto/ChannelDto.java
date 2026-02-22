package com.lazycord.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelDto {
    private UUID id;
    private String name;
    private String description;
    private String type;
    private UUID createdById;
    private String createdByUsername;
    private int memberCount;
    private LocalDateTime createdAt;
}
