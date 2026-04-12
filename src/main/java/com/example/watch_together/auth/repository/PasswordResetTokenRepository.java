package com.example.watch_together.auth.repository;

import com.example.watch_together.auth.entity.PasswordResetToken;
import com.example.watch_together.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findAllByUserAndUsedFalse(User user);
}