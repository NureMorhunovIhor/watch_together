package com.example.watch_together.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwoFactorSetupResponse {
    private String secret;
    private String otpauthUrl;
    private Boolean enabled;
}