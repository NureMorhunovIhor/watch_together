package com.example.watch_together.media.service;

import com.example.watch_together.media.dto.ExternalMediaResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PartnerMediaProvider {

    private final List<ExternalMediaResponse> catalog = List.of(
            ExternalMediaResponse.builder()
                    .externalId("partner-big-buck-bunny")
                    .title("Big Buck Bunny")
                    .description("Licensed demo movie from partner media provider.")
                    .sourceType("EXTERNAL_URL")
                    .sourceUrl("https://www.w3schools.com/html/mov_bbb.mp4")
                    .thumbnailUrl("https://peach.blender.org/wp-content/uploads/title_anouncement.jpg")
                    .releaseYear(2008)
                    .durationSeconds(596)
                    .provider("PARTNER_MEDIA")
                    .alreadyInCatalog(false)
                    .localMediaId(null)
                    .build(),

            ExternalMediaResponse.builder()
                    .externalId("partner-avatar-demo")
                    .title("Avatar Demo")
                    .description("Demo partner movie for WatchTogether integration.")
                    .sourceType("EXTERNAL_URL")
                    .sourceUrl("https://www.w3schools.com/html/movie.mp4")
                    .thumbnailUrl(null)
                    .releaseYear(2026)
                    .durationSeconds(600)
                    .provider("PARTNER_MEDIA")
                    .alreadyInCatalog(false)
                    .localMediaId(null)
                    .build(),

            ExternalMediaResponse.builder()
                    .externalId("partner-space-journey")
                    .title("Space Journey")
                    .description("Demo sci-fi partner media item.")
                    .sourceType("EXTERNAL_URL")
                    .sourceUrl("https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4")
                    .thumbnailUrl(null)
                    .releaseYear(2024)
                    .durationSeconds(600)
                    .provider("PARTNER_MEDIA")
                    .alreadyInCatalog(false)
                    .localMediaId(null)
                    .build()
    );

    public List<ExternalMediaResponse> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();

        if (q.isBlank()) {
            return List.of();
        }

        return catalog.stream()
                .filter(item ->
                        contains(item.getTitle(), q)
                                || contains(item.getDescription(), q)
                                || contains(item.getProvider(), q)
                )
                .toList();
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }
}