package com.example.watch_together.media.repository;

import com.example.watch_together.media.entity.MediaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaItemRepository extends JpaRepository<MediaItem, Long> {

    List<MediaItem> findAllByIsPublicTrueOrderByIdDesc();

    List<MediaItem> findAllByIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdDesc(String title);

    Optional<MediaItem> findByIdAndIsPublicTrue(Long id);
    List<MediaItem> findTop10ByTitleContainingIgnoreCase(String title);

    boolean existsBySourceUrl(String sourceUrl);

    Optional<MediaItem> findFirstBySourceUrl(String sourceUrl);
}