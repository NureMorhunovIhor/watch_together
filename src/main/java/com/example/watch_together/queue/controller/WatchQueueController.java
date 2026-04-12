package com.example.watch_together.queue.controller;

import com.example.watch_together.media.dto.ExternalQueueMediaRequest;
import com.example.watch_together.queue.dto.AddToQueueRequest;
import com.example.watch_together.queue.dto.MoveQueueItemRequest;
import com.example.watch_together.queue.dto.QueueItemResponse;
import com.example.watch_together.queue.service.WatchQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/rooms/{roomCode}/queue")
@RequiredArgsConstructor
public class WatchQueueController {

    private final WatchQueueService watchQueueService;

    @PostMapping
    public ResponseEntity<QueueItemResponse> addToQueue(@PathVariable String roomCode,
                                                        @RequestBody AddToQueueRequest request,
                                                        Principal principal) {
        return ResponseEntity.ok(watchQueueService.addToQueue(roomCode, request, principal));
    }

    @GetMapping
    public ResponseEntity<List<QueueItemResponse>> getQueue(@PathVariable String roomCode,
                                                            Principal principal) {
        return ResponseEntity.ok(watchQueueService.getQueue(roomCode, principal));
    }

    @DeleteMapping("/{queueItemId}")
    public ResponseEntity<Void> removeFromQueue(@PathVariable String roomCode,
                                                @PathVariable Long queueItemId,
                                                Principal principal) {
        watchQueueService.removeFromQueue(roomCode, queueItemId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{queueItemId}/play")
    public ResponseEntity<QueueItemResponse> playQueueItem(@PathVariable String roomCode,
                                                           @PathVariable Long queueItemId,
                                                           Principal principal) {
        return ResponseEntity.ok(watchQueueService.playQueueItem(roomCode, queueItemId, principal));
    }

    @PostMapping("/next")
    public ResponseEntity<QueueItemResponse> playNext(@PathVariable String roomCode,
                                                      Principal principal) {
        return ResponseEntity.ok(watchQueueService.playNext(roomCode, principal));
    }

    @PatchMapping("/{queueItemId}/move")
    public ResponseEntity<List<QueueItemResponse>> moveQueueItem(@PathVariable String roomCode,
                                                                 @PathVariable Long queueItemId,
                                                                 @RequestBody MoveQueueItemRequest request,
                                                                 Principal principal) {
        return ResponseEntity.ok(watchQueueService.moveQueueItem(roomCode, queueItemId, request, principal));
    }

    @PostMapping("/external")
    public ResponseEntity<QueueItemResponse> addExternalMediaToQueue(@PathVariable String roomCode,
                                                                     @RequestBody ExternalQueueMediaRequest request,
                                                                     Principal principal) {
        return ResponseEntity.ok(watchQueueService.addExternalMediaToQueue(roomCode, request, principal));
    }
}