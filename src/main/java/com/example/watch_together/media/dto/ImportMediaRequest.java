package com.example.watch_together.media.dto;

import lombok.Data;

@Data
public class ImportMediaRequest {
    private String externalId;
    private String title;
    private String description;
    private String sourceType;
    private String sourceUrl;
    private String thumbnailUrl;
    private Integer releaseYear;
    private Integer durationSeconds;
    private String provider;
}