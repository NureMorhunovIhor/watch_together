package com.example.watch_together.chat.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {
    private String roomCode;
    private String content;
    private Long replyToMessageId;
}