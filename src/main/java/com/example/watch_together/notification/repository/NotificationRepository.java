package com.example.watch_together.notification.repository;

import com.example.watch_together.notification.entity.Notification;
import com.example.watch_together.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUserOrderByCreatedAtDesc(User user);
    Optional<Notification> findByIdAndUser(Long id, User user);
}