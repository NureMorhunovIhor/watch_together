package com.example.watch_together.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TwoFactorLoginRequest {

    @NotBlank
    private String tempToken;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Code must contain 6 digits")
    private String code;
}