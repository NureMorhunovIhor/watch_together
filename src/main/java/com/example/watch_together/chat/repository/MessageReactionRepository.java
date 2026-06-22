package com.example.watch_together.chat.repository;

import com.example.watch_together.chat.entity.ChatMessage;
import com.example.watch_together.chat.entity.MessageReaction;
import com.example.watch_together.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.watch_together.room.entity.WatchRoom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    List<MessageReaction> findAllByMessage(ChatMessage message);
    Optional<MessageReaction> findByMessageAndUserAndReactionType(ChatMessage message, User user, String reactionType);
    void deleteByMessageAndUserAndReactionType(ChatMessage message, User user, String reactionType);
    @Query("select count(r) from MessageReaction r where r.message.room = :room")
    long countByRoom(@Param("room") WatchRoom room);
    long countByMessageRoom(WatchRoom room);
}