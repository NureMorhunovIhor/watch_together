package com.example.watch_together.playback.controller;

import com.example.watch_together.playback.dto.PlaybackEventResponse;
import com.example.watch_together.playback.dto.PlaybackStateResponse;
import com.example.watch_together.playback.service.PlaybackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/playback")
@RequiredArgsConstructor
public class PlaybackRestController {

    private final PlaybackService playbackService;

    @GetMapping("/rooms/{roomCode}/state")
    public ResponseEntity<PlaybackStateResponse> getCurrentState(@PathVariable String roomCode,
                                                                 Principal principal) {
        return ResponseEntity.ok(playbackService.getCurrentState(roomCode, principal));
    }

    @GetMapping("/rooms/{roomCode}/events")
    public ResponseEntity<List<PlaybackEventResponse>> getPlaybackEvents(@PathVariable String roomCode,
                                                                         Principal principal) {
        return ResponseEntity.ok(playbackService.getPlaybackEvents(roomCode, principal));
    }
}