package com.lazycord.service;

import com.lazycord.model.Channel;
import com.lazycord.model.Community;
import com.lazycord.model.Message;
import com.lazycord.model.User;
import com.lazycord.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional
    public Message saveMessage(String content, User sender, Channel channel, Community community) {
        Message message = new Message();
        message.setContent(content);
        message.setSender(sender);
        message.setChannel(channel);
        message.setCommunity(community);  // Set community
        message.setType(Message.MessageType.TEXT);

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getChannelMessages(UUID channelId, Community community) {
        Channel channel = new Channel();
        channel.setId(channelId);
        channel.setCommunity(community);
        return messageRepository.findByChannelAndCommunityOrderByCreatedAtAsc(channel);
    }

    @Transactional(readOnly = true)
    public List<Message> getChannelMessagesRecent(UUID channelId, Community community) {
        Channel channel = new Channel();
        channel.setId(channelId);
        channel.setCommunity(community);
        return messageRepository.findTop50ByChannelAndCommunityOrderByCreatedAtDesc(channel);
    }

    @Transactional
    public Message editMessage(UUID messageId, String newContent, User editor) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSender().getId().equals(editor.getId())) {
            throw new RuntimeException("Not authorized to edit this message");
        }

        message.setContent(newContent);
        message.setEdited(true);

        return messageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(UUID messageId, User deleter) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSender().getId().equals(deleter.getId())) {
            throw new RuntimeException("Not authorized to delete this message");
        }

        messageRepository.delete(message);
    }
}
