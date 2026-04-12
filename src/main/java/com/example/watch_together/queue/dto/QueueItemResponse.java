package com.example.watch_together.queue.dto;

import com.example.watch_together.queue.entity.QueueStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QueueItemResponse {
    private Long queueItemId;
    private String roomCode;
    private Long mediaId;
    private String mediaTitle;
    private String sourceUrl;
    private Integer queueOrder;
    private QueueStatus status;
    private Long addedByUserId;
    private String addedByUsername;
    private LocalDateTime addedAt;
    private LocalDateTime playedAt;
}