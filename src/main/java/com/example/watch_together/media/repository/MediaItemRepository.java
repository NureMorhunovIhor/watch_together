package com.example.watch_together.media.repository;

import com.example.watch_together.media.entity.MediaItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaItemRepository extends JpaRepository<MediaItem, Long> {
}