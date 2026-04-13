package com.example.watch_together.notification.controller;

import com.example.watch_together.notification.dto.NotificationResponse;
import com.example.watch_together.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Principal principal) {
        return ResponseEntity.ok(notificationService.getMyNotifications(principal));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId, Principal principal) {
        notificationService.markAsRead(notificationId, principal);
        return ResponseEntity.noContent().build();
    }
}