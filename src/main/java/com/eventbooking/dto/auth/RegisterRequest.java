package com.eventbooking.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String fullName;

    @NotBlank
    private String password;

    @NotBlank
    @Email
    private String email;
}
