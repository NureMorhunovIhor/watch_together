package com.example.watch_together.room.controller;

import com.example.watch_together.room.dto.*;
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

    @PutMapping("/{code}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable String code,
                                                   @RequestBody UpdateRoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(code, request));
    }

    @PostMapping("/{code}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable String code) {
        roomService.leaveRoom(code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/kick/{userId}")
    public ResponseEntity<Void> kickUser(@PathVariable String code,
                                         @PathVariable Long userId) {
        roomService.kickUser(code, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/transfer-host/{userId}")
    public ResponseEntity<Void> transferHost(@PathVariable String code,
                                             @PathVariable Long userId) {
        roomService.transferHost(code, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/regenerate-code")
    public ResponseEntity<String> regenerateCode(@PathVariable String code) {
        return ResponseEntity.ok(roomService.regenerateCode(code));
    }

    @GetMapping("/public")
    public ResponseEntity<List<RoomResponse>> getPublicRooms() {
        return ResponseEntity.ok(roomService.getPublicRooms());
    }

    @GetMapping("/search")
    public ResponseEntity<List<RoomResponse>> searchRooms(@RequestParam String query) {
        return ResponseEntity.ok(roomService.searchRooms(query));
    }

    @PostMapping("/{code}/invite")
    public ResponseEntity<Void> inviteUser(@PathVariable String code,
                                           @RequestBody InviteRequest request) {
        roomService.inviteUser(code, request.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/accept-invite")
    public ResponseEntity<Void> acceptInvite(@PathVariable String code) {
        roomService.acceptInvite(code);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> closeRoom(@PathVariable String code) {
        roomService.closeRoom(code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/grant-control/{userId}")
    public ResponseEntity<Void> grantControl(@PathVariable String code,
                                             @PathVariable Long userId) {
        roomService.grantPlaybackControl(code, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/revoke-control/{userId}")
    public ResponseEntity<Void> revokeControl(@PathVariable String code,
                                              @PathVariable Long userId) {
        roomService.revokePlaybackControl(code, userId);
        return ResponseEntity.ok().build();
    }
}