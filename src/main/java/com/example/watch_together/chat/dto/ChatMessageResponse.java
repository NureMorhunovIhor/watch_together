package com.example.watch_together.chat.dto;

import com.example.watch_together.chat.entity.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private String roomCode;
    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private MessageType messageType;
    private String content;
    private Long replyToMessageId;
    private Boolean edited;
    private Boolean deleted;
    private LocalDateTime sentAt;
    private LocalDateTime editedAt;
    private List<MessageReactionResponse> reactions;
}