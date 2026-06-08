package com.eventbooking.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
