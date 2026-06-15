package com.example.watch_together.media.service;

import com.example.watch_together.media.dto.ExternalMediaResponse;
import com.example.watch_together.media.dto.ImportMediaRequest;
import com.example.watch_together.media.entity.MediaItem;
import com.example.watch_together.media.repository.MediaItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalMediaService {

    private final MediaItemRepository mediaItemRepository;
    private final PartnerMediaProvider partnerMediaProvider;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${youtube.api-key:}")
    private String youtubeApiKey;

    public List<ExternalMediaResponse> search(String query) {
        if (query == null || query.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query is required");
        }

        String cleanQuery = query.trim();

        List<ExternalMediaResponse> result = new ArrayList<>();

        List<MediaItem> local = mediaItemRepository.findTop10ByTitleContainingIgnoreCase(cleanQuery);

        for (MediaItem item : local) {
            result.add(ExternalMediaResponse.builder()
                    .externalId("local-" + item.getId())
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .sourceType(item.getSourceType())
                    .sourceUrl(item.getSourceUrl())
                    .thumbnailUrl(item.getThumbnailUrl())
                    .releaseYear(item.getReleaseYear())
                    .durationSeconds(item.getDurationSeconds())
                    .provider("LOCAL")
                    .alreadyInCatalog(true)
                    .localMediaId(item.getId())
                    .build());
        }

        if (!local.isEmpty()) {
            return result;
        }

        result.addAll(searchYouTube(cleanQuery));
        result.addAll(partnerMediaProvider.search(cleanQuery));

        return result;
    }

    private List<ExternalMediaResponse> searchYouTube(String query) {
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            System.out.println("YouTube API key is missing. Skipping YouTube search.");
            return List.of();
        }

        String url = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/youtube/v3/search")
                .queryParam("part", "snippet")
                .queryParam("type", "video")
                .queryParam("maxResults", 5)
                .queryParam("q", query + " official trailer")
                .queryParam("key", youtubeApiKey)
                .build()
                .toUriString();

        try {
            Map<?, ?> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !(response.get("items") instanceof List<?> items)) {
                return List.of();
            }

            List<ExternalMediaResponse> results = new ArrayList<>();

            for (Object obj : items) {
                if (!(obj instanceof Map<?, ?> item)) continue;

                Object idObj = item.get("id");
                Object snippetObj = item.get("snippet");

                if (!(idObj instanceof Map<?, ?> id)) continue;
                if (!(snippetObj instanceof Map<?, ?> snippet)) continue;

                Object videoIdObj = id.get("videoId");

                if (videoIdObj == null) continue;

                String videoId = videoIdObj.toString();

                String title = snippet.get("title") != null
                        ? snippet.get("title").toString()
                        : query + " trailer";

                String description = snippet.get("description") != null
                        ? snippet.get("description").toString()
                        : "";

                String thumbnailUrl = extractYoutubeThumbnail(snippet);

                results.add(ExternalMediaResponse.builder()
                        .externalId("youtube-" + videoId)
                        .title(title)
                        .description(description)
                        .sourceType("YOUTUBE")
                        .sourceUrl("https://www.youtube.com/watch?v=" + videoId)
                        .thumbnailUrl(thumbnailUrl)
                        .releaseYear(null)
                        .durationSeconds(600)
                        .provider("YOUTUBE")
                        .alreadyInCatalog(false)
                        .localMediaId(null)
                        .build());
            }

            return results;
        } catch (Exception e) {
            System.out.println("YouTube search failed: " + e.getMessage());
            return List.of();
        }
    }

    private String extractYoutubeThumbnail(Map<?, ?> snippet) {
        Object thumbnailsObj = snippet.get("thumbnails");

        if (!(thumbnailsObj instanceof Map<?, ?> thumbnails)) {
            return null;
        }

        Object highObj = thumbnails.get("high");
        Object mediumObj = thumbnails.get("medium");
        Object defaultObj = thumbnails.get("default");

        Object selected = highObj != null
                ? highObj
                : mediumObj != null
                ? mediumObj
                : defaultObj;

        if (!(selected instanceof Map<?, ?> selectedMap)) {
            return null;
        }

        Object url = selectedMap.get("url");

        return url != null ? url.toString() : null;
    }

    public MediaItem importMedia(ImportMediaRequest request) {
        if (request.getSourceUrl() == null || request.getSourceUrl().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source URL is required");
        }

        if (request.getTitle() == null || request.getTitle().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media title is required");
        }

        String cleanSourceUrl = request.getSourceUrl().trim();

        return mediaItemRepository.findFirstBySourceUrl(cleanSourceUrl)
                .orElseGet(() -> {
                    String sourceType = resolveSourceType(request.getSourceType(), cleanSourceUrl);
                    String mediaType = resolveMediaType(sourceType);

                    MediaItem item = new MediaItem();

                    item.setTitle(request.getTitle().trim());
                    item.setDescription(request.getDescription());
                    item.setMediaType(mediaType);
                    item.setSourceType(sourceType);
                    item.setSourceUrl(cleanSourceUrl);
                    item.setThumbnailUrl(request.getThumbnailUrl());
                    item.setDurationSeconds(
                            request.getDurationSeconds() != null
                                    ? request.getDurationSeconds()
                                    : 600
                    );
                    item.setReleaseYear(request.getReleaseYear());
                    item.setLanguageCode("en");
                    item.setAgeRating(null);
                    item.setIsPublic(true);

                    return mediaItemRepository.save(item);
                });
    }

    private String resolveSourceType(String sourceType, String sourceUrl) {
        if (sourceType != null && !sourceType.isBlank()) {
            String normalized = sourceType.trim().toUpperCase();

            if (normalized.equals("YOUTUBE") || normalized.equals("EXTERNAL_URL")) {
                return normalized;
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported source type: " + sourceType
            );
        }

        String url = sourceUrl.toLowerCase();

        if (url.contains("youtube.com/watch") || url.contains("youtu.be/")) {
            return "YOUTUBE";
        }

        return "EXTERNAL_URL";
    }

    private String resolveMediaType(String sourceType) {
        if ("YOUTUBE".equals(sourceType)) {
            return "YOUTUBE";
        }

        return "STREAM_LINK";
    }
}