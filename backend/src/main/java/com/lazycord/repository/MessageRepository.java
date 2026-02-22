package com.lazycord.repository;

import com.lazycord.model.Channel;
import com.lazycord.model.Message;
import com.lazycord.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByChannelOrderByCreatedAtAsc(Channel channel);

    Page<Message> findByChannelOrderByCreatedAtDesc(Channel channel, Pageable pageable);

    List<Message> findBySenderOrderByCreatedAtDesc(User sender);

    List<Message> findTop50ByChannelOrderByCreatedAtDesc(Channel channel);
}
