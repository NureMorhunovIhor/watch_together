package com.example.watch_together.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100)
    private String displayName;

    @Size(min = 3, max = 50)
    private String username;

    @Email
    @Size(max = 120)
    private String email;
}