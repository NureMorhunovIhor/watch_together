package com.example.watch_together.security;

import com.example.watch_together.role.entity.Role;
import com.example.watch_together.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        String secret = "01234567890123456789012345678901";
        jwtService = new JwtService(secret, 60 * 60 * 1000, 7L * 24 * 60 * 60 * 1000);

        Role role = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .description("Default user role")
                .build();

        User user = User.builder()
                .id(10L)
                .username("igor")
                .email("igor@example.com")
                .passwordHash("encoded-password")
                .displayName("Igor")
                .isActive(true)
                .roles(Set.of(role))
                .build();

        userDetails = new CustomUserDetails(user);
    }

    @Test
    void generateAccessToken_shouldContainUserDataAndAccessType() {
        String token = jwtService.generateAccessToken(userDetails);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("igor@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(10L);
        assertThat(jwtService.extractTokenType(token)).isEqualTo("access");
    }

    @Test
    void generateRefreshToken_shouldContainRefreshType() {
        String token = jwtService.generateRefreshToken(userDetails);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("igor@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(10L);
        assertThat(jwtService.extractTokenType(token)).isEqualTo("refresh");
    }

    @Test
    void generateTempTwoFactorToken_shouldBeDetectedAsTemp2fa() {
        String token = jwtService.generateTempTwoFactorToken(userDetails);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractTokenType(token)).isEqualTo("temp_2fa");
        assertThat(jwtService.isTempTwoFactorToken(token)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalseForInvalidToken() {
        assertThat(jwtService.isTokenValid("not-a-valid-token")).isFalse();
    }
}
