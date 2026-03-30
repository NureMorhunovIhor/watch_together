package com.example.watch_together.room.controller;

import com.example.watch_together.room.dto.CreateRoomRequest;
import com.example.watch_together.room.dto.ParticipantResponse;
import com.example.watch_together.room.dto.RoomResponse;
import com.example.watch_together.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(roomService.createRoom(request));
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code) {
        return ResponseEntity.ok(roomService.getByCode(code));
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<RoomResponse> joinRoom(@PathVariable String code) {
        return ResponseEntity.ok(roomService.joinRoom(code));
    }

    @GetMapping("/{code}/participants")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(@PathVariable String code) {
        return ResponseEntity.ok(roomService.getParticipants(code));
    }
}