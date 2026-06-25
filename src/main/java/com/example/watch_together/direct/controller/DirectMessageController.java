package com.example.watch_together.direct.controller;

import com.example.watch_together.direct.dto.DirectMessageRequest;
import com.example.watch_together.direct.dto.DirectMessageResponse;
import com.example.watch_together.direct.service.DirectMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/direct-messages")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;

    @GetMapping("/{friendId}")
    public ResponseEntity<List<DirectMessageResponse>> getMessages(@PathVariable Long friendId,
                                                                   Principal principal) {
        return ResponseEntity.ok(directMessageService.getMessages(friendId, principal));
    }

    @PostMapping("/{friendId}")
    public ResponseEntity<DirectMessageResponse> sendMessage(@PathVariable Long friendId,
                                                             @RequestBody DirectMessageRequest request,
                                                             Principal principal) {
        return ResponseEntity.ok(directMessageService.sendMessage(friendId, request, principal));
    }
}