package com.example.watch_together.media.controller;

import com.example.watch_together.media.dto.CreateMediaRequest;
import com.example.watch_together.media.dto.ExternalMediaResponse;
import com.example.watch_together.media.dto.ImportMediaRequest;
import com.example.watch_together.media.dto.MediaResponse;
import com.example.watch_together.media.entity.MediaItem;
import com.example.watch_together.media.service.ExternalMediaService;
import com.example.watch_together.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final ExternalMediaService externalMediaService;

    @PostMapping
    public ResponseEntity<MediaResponse> createMedia(@RequestBody CreateMediaRequest request) {
        return ResponseEntity.ok(mediaService.createMedia(request));
    }

    @GetMapping
    public ResponseEntity<List<MediaResponse>> getCatalog() {
        return ResponseEntity.ok(mediaService.getPublicCatalog());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ExternalMediaResponse>> searchMedia(@RequestParam String query) {
        return ResponseEntity.ok(externalMediaService.search(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaResponse> getMedia(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.getPublicMediaById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedia(@PathVariable Long id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public MediaItem importMedia(@RequestBody ImportMediaRequest request) {
        return externalMediaService.importMedia(request);
    }
}