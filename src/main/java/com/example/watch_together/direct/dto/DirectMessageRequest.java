package com.example.watch_together.direct.dto;

import com.example.watch_together.direct.entity.DirectMessageType;
import lombok.Data;

@Data
public class DirectMessageRequest {
    private String content;
    private DirectMessageType messageType;
}