package com.example.watch_together.friend.controller;

import com.example.watch_together.friend.dto.FriendRequestResponse;
import com.example.watch_together.friend.dto.FriendResponse;
import com.example.watch_together.friend.dto.SendFriendRequestDto;
import com.example.watch_together.friend.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request")
    public ResponseEntity<FriendRequestResponse> sendRequest(@RequestBody SendFriendRequestDto request,
                                                             Principal principal) {
        return ResponseEntity.ok(friendshipService.sendRequest(request.getUserId(), principal));
    }

    @PostMapping("/requests/{friendshipId}/accept")
    public ResponseEntity<FriendRequestResponse> acceptRequest(@PathVariable Long friendshipId,
                                                               Principal principal) {
        return ResponseEntity.ok(friendshipService.acceptRequest(friendshipId, principal));
    }

    @PostMapping("/requests/{friendshipId}/decline")
    public ResponseEntity<FriendRequestResponse> declineRequest(@PathVariable Long friendshipId,
                                                                Principal principal) {
        return ResponseEntity.ok(friendshipService.declineRequest(friendshipId, principal));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<List<FriendRequestResponse>> getIncomingRequests(Principal principal) {
        return ResponseEntity.ok(friendshipService.getIncomingRequests(principal));
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<List<FriendRequestResponse>> getOutgoingRequests(Principal principal) {
        return ResponseEntity.ok(friendshipService.getOutgoingRequests(principal));
    }

    @GetMapping
    public ResponseEntity<List<FriendResponse>> getFriends(Principal principal) {
        return ResponseEntity.ok(friendshipService.getFriends(principal));
    }

    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<Void> removeFriend(@PathVariable Long friendUserId,
                                             Principal principal) {
        friendshipService.removeFriend(friendUserId, principal);
        return ResponseEntity.noContent().build();
    }
}