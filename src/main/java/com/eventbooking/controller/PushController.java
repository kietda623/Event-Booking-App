package com.eventbooking.controller;

import com.eventbooking.dto.common.ApiResponse;
import com.eventbooking.dto.push.PushSubscriptionRequest;
import com.eventbooking.dto.push.VapidPublicKeyResponse;
import com.eventbooking.push.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Tag(name = "Push Notifications")
public class PushController {
    private final PushNotificationService pushNotificationService;

    @GetMapping("/vapid-public-key")
    @Operation(summary = "Get VAPID public key")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "VAPID public key retrieved")
    public ApiResponse<VapidPublicKeyResponse> publicKey() {
        return ApiResponse.success("VAPID public key retrieved", new VapidPublicKeyResponse(pushNotificationService.publicKey()));
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe current user to push notifications")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Push subscription saved")
    public ApiResponse<Void> subscribe(@Valid @RequestBody PushSubscriptionRequest request) {
        pushNotificationService.subscribe(currentEmail(), request);
        return ApiResponse.success("Push subscription saved", null);
    }

    @DeleteMapping("/subscribe")
    @Operation(summary = "Unsubscribe current user from push notifications")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Push subscription removed")
    public ApiResponse<Void> unsubscribe(@RequestBody(required = false) PushSubscriptionRequest request) {
        pushNotificationService.unsubscribe(currentEmail(), request == null ? null : request.getEndpoint());
        return ApiResponse.success("Push subscription removed", null);
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
