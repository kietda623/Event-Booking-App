package com.eventbooking.dto.auth;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
