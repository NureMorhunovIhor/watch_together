package com.example.watch_together.playback.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlaybackCommandRequest {
    private String roomCode;
    private Long mediaId;
    private String action;
    private Integer positionSeconds;
    private BigDecimal playbackSpeed;
}