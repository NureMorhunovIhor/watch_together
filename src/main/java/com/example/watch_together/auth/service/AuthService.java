package com.example.watch_together.auth.service;

import com.example.watch_together.auth.dto.*;
import com.example.watch_together.auth.entity.PasswordResetToken;
import com.example.watch_together.auth.repository.PasswordResetTokenRepository;
import com.example.watch_together.role.entity.Role;
import com.example.watch_together.role.repository.RoleRepository;
import com.example.watch_together.security.CustomUserDetails;
import com.example.watch_together.security.JwtService;
import com.example.watch_together.security.TotpService;
import com.example.watch_together.session.entity.UserSession;
import com.example.watch_together.session.repository.UserSessionRepository;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.entity.UserStatus;
import com.example.watch_together.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

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
                .twoFactorEnabled(false)
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

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            CustomUserDetails userDetails = new CustomUserDetails(user);
            String tempToken = jwtService.generateTempTwoFactorToken(userDetails);

            return AuthResponse.builder()
                    .tokenType("Bearer")
                    .requiresTwoFactor(true)
                    .tempToken(tempToken)
                    .build();
        }

        return issueTokens(user, httpRequest);
    }

    public AuthResponse loginWithTwoFactor(TwoFactorLoginRequest request, HttpServletRequest httpRequest) {
        if (!jwtService.isTokenValid(request.getTempToken())) {
            throw new RuntimeException("Invalid temp token");
        }

        if (!jwtService.isTempTwoFactorToken(request.getTempToken())) {
            throw new RuntimeException("Token is not a 2FA temp token");
        }

        Long userId = jwtService.extractUserId(request.getTempToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new RuntimeException("2FA is disabled for this user");
        }

        String secret = user.getTwoFactorSecret();
        if (secret == null || secret.isBlank()) {
            throw new RuntimeException("2FA secret is not configured");
        }

        if (!totpService.isCodeValid(secret, request.getCode())) {
            throw new RuntimeException("Invalid 2FA code");
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
                .requiresTwoFactor(false)
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

    @Transactional
    public TwoFactorSetupResponse setupTwoFactor(Principal principal) {
        User user = getCurrentUser(principal);

        String secret = user.getTwoFactorSecret();
        if (secret == null || secret.isBlank()) {
            secret = totpService.generateSecret();
            user.setTwoFactorSecret(secret);
            userRepository.save(user);
        }

        String otpauthUrl = totpService.buildOtpAuthUrl(
                "WatchTogether",
                user.getEmail(),
                secret
        );

        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .otpauthUrl(otpauthUrl)
                .enabled(Boolean.TRUE.equals(user.getTwoFactorEnabled()))
                .build();
    }

    @Transactional
    public TwoFactorStatusResponse getTwoFactorStatus(Principal principal) {
        User user = getCurrentUser(principal);

        return TwoFactorStatusResponse.builder()
                .enabled(Boolean.TRUE.equals(user.getTwoFactorEnabled()))
                .build();
    }

    @Transactional
    public void enableTwoFactor(Principal principal, TwoFactorCodeRequest request) {
        User user = getCurrentUser(principal);

        String secret = user.getTwoFactorSecret();
        if (secret == null || secret.isBlank()) {
            throw new RuntimeException("2FA setup was not initialized");
        }

        if (!totpService.isCodeValid(secret, request.getCode())) {
            throw new RuntimeException("Invalid 2FA code");
        }

        System.out.println("2FA SECRET FROM DB = " + secret);
        System.out.println("2FA CODE FROM REQUEST = " + request.getCode());
        user.setTwoFactorEnabled(true);
        user.setTwoFactorVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void disableTwoFactor(Principal principal, TwoFactorCodeRequest request) {
        User user = getCurrentUser(principal);

        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            throw new RuntimeException("2FA is already disabled");
        }

        String secret = user.getTwoFactorSecret();
        if (secret == null || secret.isBlank()) {
            throw new RuntimeException("2FA secret not found");
        }

        if (!totpService.isCodeValid(secret, request.getCode())) {
            throw new RuntimeException("Invalid 2FA code");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setTwoFactorVerifiedAt(null);
        userRepository.save(user);
    }

    private User getCurrentUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new RuntimeException("Unauthorized");
        }

        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
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
                .requiresTwoFactor(false)
                .build();
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User with this email not found"));

        passwordResetTokenRepository.findAllByUserAndUsedFalse(user)
                .forEach(token -> {
                    token.setUsed(true);
                    token.setUsedAt(LocalDateTime.now());
                });

        String tokenValue = UUID.randomUUID().toString();

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();

        passwordResetTokenRepository.save(token);

        return ForgotPasswordResponse.builder()
                .message("Password reset token created")
                .resetToken(tokenValue)
                .resetLink("http://localhost:8080/reset-password.html?token=" + tokenValue)
                .build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (Boolean.TRUE.equals(token.getUsed())) {
            throw new RuntimeException("Reset token already used");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token expired");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        token.setUsed(true);
        token.setUsedAt(LocalDateTime.now());
    }
}