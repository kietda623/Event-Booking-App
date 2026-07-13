package com.eventbooking.dto.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PushSubscriptionRequest {
    @NotBlank
    private String endpoint;

    @Valid
    @NotNull
    private Keys keys;

    @Data
    public static class Keys {
        @NotBlank
        private String p256dh;

        @NotBlank
        private String auth;
    }
}
