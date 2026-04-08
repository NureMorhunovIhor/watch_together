package com.example.watch_together.playback.controller;

import com.example.watch_together.playback.dto.PlaybackCommandRequest;
import com.example.watch_together.playback.service.PlaybackService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class PlaybackWebSocketController {

    private final PlaybackService playbackService;

    @MessageMapping("/playback.command")
    public void handlePlaybackCommand(@Payload PlaybackCommandRequest request, Principal principal) {
        playbackService.handleCommand(request, principal);
    }
}