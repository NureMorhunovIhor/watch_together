package com.example.watch_together.room.dto;

import com.example.watch_together.room.entity.ParticipantRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParticipantResponse {
    private Long userId;
    private String username;
    private String displayName;
    private ParticipantRole role;
    private Boolean muted;
    private Boolean canControlPlayback;
}