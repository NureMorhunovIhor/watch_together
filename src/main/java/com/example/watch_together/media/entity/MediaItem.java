package com.example.watch_together.media.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "media_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "media_type", nullable = false, length = 30)
    private String mediaType;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "age_rating", length = 20)
    private String ageRating;

    @Column(name = "is_public")
    private Boolean isPublic;
}