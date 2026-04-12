package com.example.watch_together.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForgotPasswordResponse {
    private String message;
    private String resetToken;
    private String resetLink;
}