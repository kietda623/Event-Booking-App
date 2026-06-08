package com.eventbooking.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String expiresAt;
    private AuthUserResponse user;

    @Data
    @AllArgsConstructor
    public static class AuthUserResponse {
        private Long id;
        private String fullName;
        private String email;
        private String role;
    }
}
