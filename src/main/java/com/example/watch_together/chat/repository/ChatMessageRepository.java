package com.example.watch_together.chat.repository;

import com.example.watch_together.chat.entity.ChatMessage;
import com.example.watch_together.room.entity.WatchRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByRoomOrderBySentAtAsc(WatchRoom room);
    Optional<ChatMessage> findByIdAndRoom(Long id, WatchRoom room);
}