package com.example.watch_together.playback.repository;

import com.example.watch_together.playback.entity.PlaybackEvent;
import com.example.watch_together.room.entity.WatchRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaybackEventRepository extends JpaRepository<PlaybackEvent, Long> {
    List<PlaybackEvent> findAllByRoomOrderByCreatedAtDesc(WatchRoom room);
}