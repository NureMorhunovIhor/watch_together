package com.example.watch_together.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String login;

    @NotBlank
    private String password;
}