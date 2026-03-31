package com.example.watch_together.chat.controller;

import com.example.watch_together.chat.dto.*;
import com.example.watch_together.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @GetMapping("/rooms/{roomCode}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getRoomMessages(@PathVariable String roomCode,
                                                                     Principal principal) {
        return ResponseEntity.ok(chatService.getRoomMessages(roomCode, principal));
    }

    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<ChatMessageResponse> editMessage(@PathVariable Long messageId,
                                                           @RequestBody EditMessageRequest request,
                                                           Principal principal) {
        return ResponseEntity.ok(chatService.editMessage(messageId, request, principal));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId,
                                              Principal principal) {
        chatService.deleteMessage(messageId, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<ChatMessageResponse> addReaction(@PathVariable Long messageId,
                                                           @RequestBody ReactionRequest request,
                                                           Principal principal) {
        return ResponseEntity.ok(chatService.addReaction(messageId, request, principal));
    }

    @DeleteMapping("/messages/{messageId}/reactions")
    public ResponseEntity<ChatMessageResponse> removeReaction(@PathVariable Long messageId,
                                                              @RequestBody ReactionRequest request,
                                                              Principal principal) {
        return ResponseEntity.ok(chatService.removeReaction(messageId, request, principal));
    }
}