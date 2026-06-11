package com.example.watch_together.billing.repository;

import com.example.watch_together.billing.entity.UserSubscription;
import com.example.watch_together.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    Optional<UserSubscription> findTopByUserOrderByStartedAtDesc(User user);
}