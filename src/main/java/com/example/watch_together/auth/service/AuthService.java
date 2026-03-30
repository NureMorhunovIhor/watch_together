package com.example.watch_together.auth.service;

import com.example.watch_together.auth.dto.*;
import com.example.watch_together.role.entity.Role;
import com.example.watch_together.role.repository.RoleRepository;
import com.example.watch_together.security.*;
import com.example.watch_together.session.entity.UserSession;
import com.example.watch_together.session.repository.UserSessionRepository;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.entity.UserStatus;
import com.example.watch_together.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already in use");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .status(UserStatus.OFFLINE)
                .isActive(true)
                .isEmailVerified(false)
                .roles(Set.of(userRole))
                .build();

        user = userRepository.save(user);

        return issueTokens(user, httpRequest);
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getLogin())
                .or(() -> userRepository.findByUsername(request.getLogin()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return issueTokens(user, httpRequest);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        UserSession session = userSessionRepository.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (Boolean.TRUE.equals(session.getIsRevoked())) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        if (!jwtService.isTokenValid(request.getRefreshToken())) {
            throw new RuntimeException("Invalid refresh token");
        }

        User user = session.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        userSessionRepository.findByRefreshToken(request.getRefreshToken())
                .ifPresent(session -> {
                    session.setIsRevoked(true);
                    session.setRevokedAt(LocalDateTime.now());
                    userSessionRepository.save(session);
                });
    }

    private AuthResponse issueTokens(User user, HttpServletRequest httpRequest) {
        CustomUserDetails userDetails = new CustomUserDetails(user);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        UserSession session = UserSession.builder()
                .user(user)
                .refreshToken(refreshToken)
                .deviceInfo(httpRequest.getHeader("User-Agent"))
                .ipAddress(httpRequest.getRemoteAddr())
                .userAgent(httpRequest.getHeader("User-Agent"))
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration() / 1000))
                .isRevoked(false)
                .build();

        userSessionRepository.save(session);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}