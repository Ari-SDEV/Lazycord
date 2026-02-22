package com.lazycord.controller;

import com.lazycord.dto.ChannelDto;
import com.lazycord.dto.CreateChannelRequest;
import com.lazycord.model.Channel;
import com.lazycord.model.User;
import com.lazycord.service.ChannelService;
import com.lazycord.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:1420", "tauri://localhost"})
public class ChannelController {

    private final ChannelService channelService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<ChannelDto>> getMyChannels(Authentication authentication) {
        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Channel> channels = channelService.findUserChannels(user);
        List<ChannelDto> dtos = channels.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/public")
    public ResponseEntity<List<ChannelDto>> getPublicChannels() {
        List<Channel> channels = channelService.findAllPublicChannels();
        List<ChannelDto> dtos = channels.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<ChannelDto> createChannel(
            @RequestBody CreateChannelRequest request,
            Authentication authentication) {

        User creator = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Channel channel = channelService.createChannel(
                request.getName(),
                request.getDescription(),
                request.getType(),
                creator
        );

        return ResponseEntity.ok(convertToDto(channel));
    }

    @PostMapping("/{channelId}/join")
    public ResponseEntity<Void> joinChannel(
            @PathVariable UUID channelId,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Channel channel = channelService.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        channelService.joinChannel(channel, user);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{channelId}/leave")
    public ResponseEntity<Void> leaveChannel(
            @PathVariable UUID channelId,
            Authentication authentication) {

        User user = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Channel channel = channelService.findById(channelId)
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        channelService.leaveChannel(channel, user);

        return ResponseEntity.ok().build();
    }

    private ChannelDto convertToDto(Channel channel) {
        ChannelDto dto = new ChannelDto();
        dto.setId(channel.getId());
        dto.setName(channel.getName());
        dto.setDescription(channel.getDescription());
        dto.setType(channel.getType().name());
        dto.setCreatedById(channel.getCreatedBy().getId());
        dto.setCreatedByUsername(channel.getCreatedBy().getUsername());
        dto.setMemberCount(channel.getMembers().size());
        dto.setCreatedAt(channel.getCreatedAt());
        return dto;
    }
}
