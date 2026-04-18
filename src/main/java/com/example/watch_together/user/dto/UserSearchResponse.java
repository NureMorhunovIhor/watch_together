package com.example.watch_together.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSearchResponse {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String relationStatus;
}