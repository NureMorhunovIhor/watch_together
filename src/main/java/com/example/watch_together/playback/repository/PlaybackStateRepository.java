package com.example.watch_together.playback.repository;

import com.example.watch_together.playback.entity.PlaybackState;
import com.example.watch_together.room.entity.WatchRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaybackStateRepository extends JpaRepository<PlaybackState, Long> {
    Optional<PlaybackState> findByRoom(WatchRoom room);
}