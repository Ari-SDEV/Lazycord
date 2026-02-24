package com.lazycord.controller;

import com.lazycord.dto.ChatMessageDto;
import com.lazycord.model.Channel;
import com.lazycord.model.Community;
import com.lazycord.model.Message;
import com.lazycord.service.ChannelService;
import com.lazycord.service.CommunityService;
import com.lazycord.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:1420", "tauri://localhost"})
public class MessageController {

    private final MessageService messageService;
    private final ChannelService channelService;
    private final CommunityService communityService;

    @GetMapping("/channel/{channelId}")
    public ResponseEntity<List<ChatMessageDto>> getChannelMessages(
            @PathVariable UUID channelId,
            @RequestParam UUID communityId) {
        
        Community community = communityService.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Community not found"));
                
        Channel channel = channelService.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
        
        // Verify channel belongs to community
        if (!channel.getCommunity().getId().equals(communityId)) {
            throw new RuntimeException("Channel does not belong to community");
        }

        List<Message> messages = messageService.getChannelMessagesRecent(channelId, community);
        List<ChatMessageDto> dtos = messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    private ChatMessageDto convertToDto(Message message) {
        ChatMessageDto dto = new ChatMessageDto();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setType(message.getType().name());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setSenderAvatarUrl(message.getSender().getAvatarUrl());
        dto.setChannelId(message.getChannel().getId());
        dto.setAttachmentUrl(message.getAttachmentUrl());
        dto.setEdited(message.getEdited());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}
