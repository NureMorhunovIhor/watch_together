package com.example.watch_together.playback.entity;

import com.example.watch_together.room.entity.WatchRoom;
import com.example.watch_together.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "playback_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaybackState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private WatchRoom room;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "playback_status", nullable = false, length = 20)
    private PlaybackStatus playbackStatus;

    @Column(name = "current_position_seconds", nullable = false)
    private Integer currentPositionSeconds;

    @Column(name = "playback_speed", nullable = false, precision = 3, scale = 2)
    private BigDecimal playbackSpeed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_action_by")
    private User lastActionBy;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;
}