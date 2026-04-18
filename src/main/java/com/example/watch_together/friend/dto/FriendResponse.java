package com.example.watch_together.friend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendResponse {
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
}