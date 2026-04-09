package com.example.watch_together.auth.controller;

import com.example.watch_together.auth.dto.*;
import com.example.watch_together.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.register(request, httpRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.login(request, httpRequest));
    }

    @PostMapping("/login/2fa")
    public ResponseEntity<AuthResponse> loginWithTwoFactor(@Valid @RequestBody TwoFactorLoginRequest request,
                                                           HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.loginWithTwoFactor(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/2fa/status")
    public ResponseEntity<TwoFactorStatusResponse> status(Principal principal) {
        return ResponseEntity.ok(authService.getTwoFactorStatus(principal));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupResponse> setup(Principal principal) {
        return ResponseEntity.ok(authService.setupTwoFactor(principal));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<Void> enable(@Valid @RequestBody TwoFactorCodeRequest request,
                                       Principal principal) {
        authService.enableTwoFactor(principal, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Void> disable(@Valid @RequestBody TwoFactorCodeRequest request,
                                        Principal principal) {
        authService.disableTwoFactor(principal, request);
        return ResponseEntity.ok().build();
    }
}