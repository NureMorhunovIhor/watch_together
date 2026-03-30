package com.example.watch_together.room.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private WatchRoom room;

    @Column(name = "allow_participant_control", nullable = false)
    private Boolean allowParticipantControl;

    @Column(name = "allow_chat", nullable = false)
    private Boolean allowChat;

    @Column(name = "allow_reactions", nullable = false)
    private Boolean allowReactions;

    @Column(name = "allow_voice_chat", nullable = false)
    private Boolean allowVoiceChat;

    @Column(name = "allow_video_chat", nullable = false)
    private Boolean allowVideoChat;

    @Column(name = "auto_pause_on_buffer", nullable = false)
    private Boolean autoPauseOnBuffer;

    @Column(name = "show_subtitles", nullable = false)
    private Boolean showSubtitles;

    @Column(name = "subtitles_language", length = 10)
    private String subtitlesLanguage;

    @Column(name = "playback_speed", nullable = false, precision = 3, scale = 2)
    private BigDecimal playbackSpeed;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}