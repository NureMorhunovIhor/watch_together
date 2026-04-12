package com.example.watch_together.media.service;

import com.example.watch_together.media.dto.CreateMediaRequest;
import com.example.watch_together.media.dto.MediaResponse;
import com.example.watch_together.media.entity.MediaItem;
import com.example.watch_together.media.repository.MediaItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaItemRepository mediaItemRepository;

    @Transactional
    public MediaResponse createMedia(CreateMediaRequest request) {
        validateRequest(request);

        MediaItem media = MediaItem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .mediaType(request.getMediaType())
                .sourceType(request.getSourceType())
                .sourceUrl(request.getSourceUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .durationSeconds(request.getDurationSeconds())
                .releaseYear(request.getReleaseYear())
                .languageCode(request.getLanguageCode())
                .ageRating(request.getAgeRating())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .build();

        media = mediaItemRepository.save(media);

        return map(media);
    }

    public List<MediaResponse> getPublicCatalog() {
        return mediaItemRepository.findAllByIsPublicTrueOrderByIdDesc()
                .stream()
                .map(this::map)
                .toList();
    }

    public List<MediaResponse> searchPublicCatalog(String query) {
        return mediaItemRepository.findAllByIsPublicTrueAndTitleContainingIgnoreCaseOrderByIdDesc(query)
                .stream()
                .map(this::map)
                .toList();
    }

    public MediaResponse getPublicMediaById(Long id) {
        MediaItem media = mediaItemRepository.findByIdAndIsPublicTrue(id)
                .orElseThrow(() -> new RuntimeException("Media not found"));
        return map(media);
    }

    @Transactional
    public void deleteMedia(Long id) {
        MediaItem media = mediaItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found"));

        mediaItemRepository.delete(media);
    }

    private void validateRequest(CreateMediaRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RuntimeException("Title is required");
        }
        if (request.getMediaType() == null || request.getMediaType().isBlank()) {
            throw new RuntimeException("Media type is required");
        }
        if (request.getSourceType() == null || request.getSourceType().isBlank()) {
            throw new RuntimeException("Source type is required");
        }
        if (request.getSourceUrl() == null || request.getSourceUrl().isBlank()) {
            throw new RuntimeException("Source URL is required");
        }
    }

    private MediaResponse map(MediaItem media) {
        return MediaResponse.builder()
                .id(media.getId())
                .title(media.getTitle())
                .description(media.getDescription())
                .mediaType(media.getMediaType())
                .sourceType(media.getSourceType())
                .sourceUrl(media.getSourceUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .durationSeconds(media.getDurationSeconds())
                .releaseYear(media.getReleaseYear())
                .languageCode(media.getLanguageCode())
                .ageRating(media.getAgeRating())
                .isPublic(media.getIsPublic())
                .build();
    }
}