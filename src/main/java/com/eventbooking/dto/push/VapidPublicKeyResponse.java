package com.eventbooking.dto.push;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VapidPublicKeyResponse {
    private String publicKey;
}
