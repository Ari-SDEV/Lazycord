package com.lazycord.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private UUID id;
    private String content;
    private String type;
    private UUID senderId;
    private String senderUsername;
    private String senderAvatarUrl;
    private UUID channelId;
    private String attachmentUrl;
    private Boolean edited;
    private LocalDateTime createdAt;
}
