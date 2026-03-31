package com.example.watch_together.room.repository;

import com.example.watch_together.room.entity.RoomSettings;
import com.example.watch_together.room.entity.WatchRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomSettingsRepository extends JpaRepository<RoomSettings, Long> {
    Optional<RoomSettings> findByRoom(WatchRoom room);
}