package com.lazycord.controller;

import com.lazycord.dto.ChatMessageDto;
import com.lazycord.model.Channel;
import com.lazycord.model.Message;
import com.lazycord.model.User;
import com.lazycord.service.ChannelService;
import com.lazycord.service.MessageService;
import com.lazycord.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ChannelService channelService;
    private final UserService userService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDto messageDto, Principal principal) {
        log.info("Received message from {} for channel {}", principal.getName(), messageDto.getChannelId());

        User sender = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Channel channel = channelService.findById(messageDto.getChannelId())
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        Message savedMessage = messageService.saveMessage(messageDto.getContent(), sender, channel);

        ChatMessageDto responseDto = convertToDto(savedMessage);

        messagingTemplate.convertAndSend("/topic/channel/" + channel.getId(), responseDto);
        log.info("Message sent to channel {}", channel.getId());
    }

    @MessageMapping("/chat.join")
    public void joinChannel(@Payload String channelId, Principal principal) {
        log.info("User {} joining channel {}", principal.getName(), channelId);

        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Channel channel = channelService.findById(UUID.fromString(channelId))
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        channelService.joinChannel(channel, user);

        messagingTemplate.convertAndSend("/topic/channel/" + channelId + "/join",
                principal.getName() + " joined the channel");
    }

    @MessageMapping("/chat.leave")
    public void leaveChannel(@Payload String channelId, Principal principal) {
        log.info("User {} leaving channel {}", principal.getName(), channelId);

        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Channel channel = channelService.findById(UUID.fromString(channelId))
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        channelService.leaveChannel(channel, user);

        messagingTemplate.convertAndSend("/topic/channel/" + channelId + "/leave",
                principal.getName() + " left the channel");
    }

    @SubscribeMapping("/topic/channel/{channelId}")
    public void subscribeToChannel(@DestinationVariable String channelId) {
        log.info("New subscription to channel {}", channelId);
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
