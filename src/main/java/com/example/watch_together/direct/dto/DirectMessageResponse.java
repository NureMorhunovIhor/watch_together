package com.example.watch_together.direct.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DirectMessageResponse {
    private Long id;

    private Long senderId;
    private String senderUsername;
    private String senderDisplayName;
    private String senderAvatarUrl;

    private Long receiverId;
    private String receiverUsername;
    private String receiverDisplayName;
    private String receiverAvatarUrl;

    private String content;
    private String messageType;

    private Boolean read;
    private LocalDateTime sentAt;
}