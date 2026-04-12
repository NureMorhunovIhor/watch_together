package com.example.watch_together.media.dto;

import lombok.Data;

@Data
public class CreateMediaRequest {
    private String title;
    private String description;
    private String mediaType;
    private String sourceType;
    private String sourceUrl;
    private String thumbnailUrl;
    private Integer durationSeconds;
    private Integer releaseYear;
    private String languageCode;
    private String ageRating;
    private Boolean isPublic;
}