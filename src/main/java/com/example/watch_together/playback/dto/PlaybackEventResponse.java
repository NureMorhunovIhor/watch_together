package com.example.watch_together.playback.dto;

import com.example.watch_together.playback.entity.PlaybackEventType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlaybackEventResponse {
    private Long id;
    private String roomCode;
    private Long userId;
    private String username;
    private Long mediaId;
    private PlaybackEventType eventType;
    private Integer fromPositionSeconds;
    private Integer toPositionSeconds;
    private String metadata;
    private LocalDateTime createdAt;
}