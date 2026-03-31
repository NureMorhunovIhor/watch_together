package com.example.watch_together.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageReactionResponse {
    private Long userId;
    private String username;
    private String reactionType;
}