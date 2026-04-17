package com.example.watch_together.notification.service;

import com.example.watch_together.notification.dto.NotificationResponse;
import com.example.watch_together.notification.dto.UnreadCountResponse;
import com.example.watch_together.notification.entity.Notification;
import com.example.watch_together.notification.entity.NotificationType;
import com.example.watch_together.notification.repository.NotificationRepository;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(User user,
                                   NotificationType type,
                                   String title,
                                   String content,
                                   String relatedEntityType,
                                   Long relatedEntityId) {

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    public List<NotificationResponse> getMyNotifications(Principal principal) {
        User user = getUserByPrincipal(principal);

        return notificationRepository.findAllByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::map)
                .toList();
    }

    public UnreadCountResponse getUnreadCount(Principal principal) {
        User user = getUserByPrincipal(principal);

        long count = notificationRepository.countByUserAndIsReadFalse(user);

        return UnreadCountResponse.builder()
                .unreadCount(count)
                .build();
    }

    @Transactional
    public void markAsRead(Long notificationId, Principal principal) {
        User user = getUserByPrincipal(principal);

        Notification notification = notificationRepository.findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
    }

    @Transactional
    public void markAllAsRead(Principal principal) {
        User user = getUserByPrincipal(principal);

        List<Notification> unread = notificationRepository.findAllByUserAndIsReadFalse(user);

        for (Notification notification : unread) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findByEmail(principal.getName())
                .or(() -> userRepository.findByUsername(principal.getName()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private NotificationResponse map(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}