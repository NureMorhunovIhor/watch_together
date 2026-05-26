package com.example.watch_together.auth.oauth;

import com.example.watch_together.security.CustomUserDetails;
import com.example.watch_together.security.JwtService;
import com.example.watch_together.user.entity.User;
import com.example.watch_together.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String registrationId = extractRegistrationId(request);

        String provider = registrationId.toUpperCase();

        String providerId = extractProviderId(registrationId, oauthUser);
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null || email.isBlank()) {
            response.sendRedirect(frontendUrl + "/login?oauthError=email_required");
            return;
        }

        User user = userRepository
                .findByProviderAndProviderId(provider, providerId)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(() -> createUser(provider, providerId, email, name));

        user.setProvider(provider);
        user.setProviderId(providerId);

        userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        String redirectUrl = frontendUrl
                + "/oauth/success"
                + "?accessToken=" + encode(accessToken)
                + "&refreshToken=" + encode(refreshToken);

        response.sendRedirect(redirectUrl);
    }

    private User createUser(String provider, String providerId, String email, String name) {
        String baseUsername = email.split("@")[0];
        String username = makeUniqueUsername(baseUsername);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(name != null && !name.isBlank() ? name : username);
        user.setPasswordHash("OAUTH2_USER");
        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setCreatedAt(LocalDateTime.now());

        return user;
    }

    private String makeUniqueUsername(String base) {
        String cleaned = base.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();

        if (cleaned.isBlank()) {
            cleaned = "user";
        }

        String username = cleaned;
        int counter = 1;

        while (userRepository.existsByUsername(username)) {
            username = cleaned + counter;
            counter++;
        }

        return username;
    }

    private String extractProviderId(String registrationId, OAuth2User oauthUser) {
        if ("facebook".equalsIgnoreCase(registrationId)) {
            return oauthUser.getAttribute("id");
        }

        return oauthUser.getAttribute("sub");
    }

    private String extractRegistrationId(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.contains("facebook")) {
            return "facebook";
        }

        return "google";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}