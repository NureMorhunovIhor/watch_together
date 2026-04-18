package com.example.watch_together.friend.dto;

import com.example.watch_together.friend.entity.FriendshipStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FriendRequestResponse {
    private Long friendshipId;
    private Long requesterId;
    private String requesterUsername;
    private String requesterDisplayName;
    private Long addresseeId;
    private String addresseeUsername;
    private String addresseeDisplayName;
    private FriendshipStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}