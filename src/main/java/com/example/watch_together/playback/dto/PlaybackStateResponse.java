package com.example.watch_together.playback.dto;

import com.example.watch_together.playback.entity.PlaybackStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PlaybackStateResponse {
    private String roomCode;
    private Long mediaId;
    private PlaybackStatus playbackStatus;
    private Integer currentPositionSeconds;
    private BigDecimal playbackSpeed;
    private Long lastActionByUserId;
    private String lastActionByUsername;
    private LocalDateTime lastSyncedAt;
}