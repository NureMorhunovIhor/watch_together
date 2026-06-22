package com.example.watch_together.chat.dto;

import com.example.watch_together.chat.entity.MessageType;
import lombok.Data;

@Data
public class ChatMessageRequest {
    private String roomCode;
    private String content;
    private Long replyToMessageId;
    private MessageType messageType;
}