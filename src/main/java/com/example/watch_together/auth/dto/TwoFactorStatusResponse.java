package com.example.watch_together.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwoFactorStatusResponse {
    private Boolean enabled;
}