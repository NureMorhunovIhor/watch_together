package com.example.watch_together.chat.controller;

import com.example.watch_together.chat.dto.ChatMessageRequest;
import com.example.watch_together.chat.dto.ChatMessageResponse;
import com.example.watch_together.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        chatService.sendMessage(request, principal);
    }
}