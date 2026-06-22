package com.example.watch_together.room.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RoomStatsResponse {

    private String roomCode;
    private String roomName;

    private Long messagesCount;
    private Long reactionsCount;

    private Long currentParticipantsCount;

    private Long queueItemsCount;
    private Long playedItemsCount;
    private Long removedItemsCount;

    private Long currentMediaId;
    private String currentMediaTitle;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
}