package com.example.watch_together.room.repository;

import com.example.watch_together.room.entity.RoomSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomSettingsRepository extends JpaRepository<RoomSettings, Long> {
}