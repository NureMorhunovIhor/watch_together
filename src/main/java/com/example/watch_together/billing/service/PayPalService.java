package com.example.watch_together.billing.service;

import com.example.watch_together.billing.dto.BillingCycle;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayPalService {

    @Value("${paypal.mode:sandbox}")
    private String mode;

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.return-url}")
    private String returnUrl;

    @Value("${paypal.cancel-url}")
    private String cancelUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private String baseUrl() {
        return "live".equalsIgnoreCase(mode)
                ? "https://api-m.paypal.com"
                : "https://api-m.sandbox.paypal.com";
    }

    public String getAccessToken() {
        String credentials = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encoded);

        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<LinkedMultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/v1/oauth2/token",
                HttpMethod.POST,
                request,
                Map.class
        );

        Object token = response.getBody().get("access_token");

        if (token == null) {
            throw new RuntimeException("PayPal access token was not returned");
        }

        return token.toString();
    }

    public PayPalOrder createOrder(String planId, BillingCycle cycle, BigDecimal amount) {
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(
                        Map.of(
                                "description", "WatchTogether " + planId + " " + cycle,
                                "amount", Map.of(
                                        "currency_code", "USD",
                                        "value", amount.setScale(2).toString()
                                )
                        )
                ),
                "application_context", Map.of(
                        "brand_name", "WatchTogether",
                        "landing_page", "LOGIN",
                        "user_action", "PAY_NOW",
                        "return_url", returnUrl,
                        "cancel_url", cancelUrl
                )
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/v2/checkout/orders",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map responseBody = response.getBody();

        String orderId = String.valueOf(responseBody.get("id"));
        String approveUrl = extractApproveUrl(responseBody);

        if (orderId == null || approveUrl == null) {
            throw new RuntimeException("PayPal order was not created correctly");
        }

        return new PayPalOrder(orderId, approveUrl);
    }

    public void captureOrder(String orderId) {
        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        restTemplate.exchange(
                baseUrl() + "/v2/checkout/orders/" + orderId + "/capture",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(), headers),
                Map.class
        );
    }

    private String extractApproveUrl(Map responseBody) {
        Object linksObj = responseBody.get("links");

        if (!(linksObj instanceof List<?> links)) {
            return null;
        }

        for (Object item : links) {
            if (item instanceof Map<?, ?> link) {
                Object rel = link.get("rel");
                Object href = link.get("href");

                if ("approve".equals(rel) && href != null) {
                    return href.toString();
                }
            }
        }

        return null;
    }

    public record PayPalOrder(String orderId, String approveUrl) {}
}