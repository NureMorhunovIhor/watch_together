package com.example.watch_together.media.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalMediaResponse {
    private String externalId;
    private String title;
    private String description;
    private String sourceType;
    private String sourceUrl;
    private String thumbnailUrl;
    private Integer releaseYear;
    private Integer durationSeconds;
    private String provider;
    private Boolean alreadyInCatalog;
    private Long localMediaId;
}