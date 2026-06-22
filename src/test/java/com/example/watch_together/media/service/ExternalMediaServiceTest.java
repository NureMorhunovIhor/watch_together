package com.example.watch_together.media.service;

import com.example.watch_together.media.dto.ExternalMediaResponse;
import com.example.watch_together.media.dto.ImportMediaRequest;
import com.example.watch_together.media.entity.MediaItem;
import com.example.watch_together.media.repository.MediaItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalMediaServiceTest {

    @Mock
    private MediaItemRepository mediaItemRepository;

    @Mock
    private PartnerMediaProvider partnerMediaProvider;

    @InjectMocks
    private ExternalMediaService externalMediaService;

    @Test
    void search_shouldThrowBadRequestWhenQueryIsBlank() {
        assertThatThrownBy(() -> externalMediaService.search("   "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void search_shouldReturnLocalCatalogItemsFirstAndSkipExternalProviders() {
        MediaItem localItem = MediaItem.builder()
                .id(5L)
                .title("Interstellar")
                .description("Space movie")
                .sourceType("EXTERNAL_URL")
                .sourceUrl("https://example.com/interstellar.mp4")
                .thumbnailUrl("https://example.com/poster.jpg")
                .releaseYear(2014)
                .durationSeconds(7200)
                .build();

        when(mediaItemRepository.findTop10ByTitleContainingIgnoreCase("Interstellar"))
                .thenReturn(List.of(localItem));

        List<ExternalMediaResponse> result = externalMediaService.search("  Interstellar  ");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalId()).isEqualTo("local-5");
        assertThat(result.get(0).getTitle()).isEqualTo("Interstellar");
        assertThat(result.get(0).getProvider()).isEqualTo("LOCAL");
        assertThat(result.get(0).getAlreadyInCatalog()).isTrue();
        assertThat(result.get(0).getLocalMediaId()).isEqualTo(5L);

        verify(partnerMediaProvider, never()).search(any());
    }

    @Test
    void search_shouldUsePartnerProviderWhenLocalCatalogIsEmptyAndYoutubeKeyIsMissing() {
        ExternalMediaResponse partnerItem = ExternalMediaResponse.builder()
                .externalId("partner-1")
                .title("Partner video")
                .provider("PARTNER")
                .alreadyInCatalog(false)
                .build();

        when(mediaItemRepository.findTop10ByTitleContainingIgnoreCase("Matrix"))
                .thenReturn(List.of());
        when(partnerMediaProvider.search("Matrix"))
                .thenReturn(List.of(partnerItem));

        List<ExternalMediaResponse> result = externalMediaService.search("Matrix");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalId()).isEqualTo("partner-1");
        assertThat(result.get(0).getProvider()).isEqualTo("PARTNER");
    }

    @Test
    void importMedia_shouldReturnExistingItemWhenSourceUrlAlreadyExists() {
        ImportMediaRequest request = new ImportMediaRequest();
        request.setTitle("Existing video");
        request.setSourceUrl("https://example.com/video.mp4");

        MediaItem existing = MediaItem.builder()
                .id(7L)
                .title("Existing video")
                .sourceUrl("https://example.com/video.mp4")
                .build();

        when(mediaItemRepository.findFirstBySourceUrl("https://example.com/video.mp4"))
                .thenReturn(Optional.of(existing));

        MediaItem result = externalMediaService.importMedia(request);

        assertThat(result).isSameAs(existing);
        verify(mediaItemRepository, never()).save(any());
    }

    @Test
    void importMedia_shouldDetectYoutubeSourceAndSaveNewItem() {
        ImportMediaRequest request = new ImportMediaRequest();
        request.setTitle("  Trailer  ");
        request.setDescription("Description");
        request.setSourceUrl("  https://www.youtube.com/watch?v=abc123  ");
        request.setDurationSeconds(null);

        when(mediaItemRepository.findFirstBySourceUrl("https://www.youtube.com/watch?v=abc123"))
                .thenReturn(Optional.empty());
        when(mediaItemRepository.save(any(MediaItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MediaItem result = externalMediaService.importMedia(request);

        assertThat(result.getTitle()).isEqualTo("Trailer");
        assertThat(result.getSourceUrl()).isEqualTo("https://www.youtube.com/watch?v=abc123");
        assertThat(result.getSourceType()).isEqualTo("YOUTUBE");
        assertThat(result.getMediaType()).isEqualTo("YOUTUBE");
        assertThat(result.getDurationSeconds()).isEqualTo(600);
        assertThat(result.getLanguageCode()).isEqualTo("en");
        assertThat(result.getIsPublic()).isTrue();
    }

    @Test
    void importMedia_shouldRejectUnsupportedSourceType() {
        ImportMediaRequest request = new ImportMediaRequest();
        request.setTitle("Video");
        request.setSourceUrl("https://example.com/video.mp4");
        request.setSourceType("FILE_SYSTEM");

        when(mediaItemRepository.findFirstBySourceUrl("https://example.com/video.mp4"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> externalMediaService.importMedia(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
