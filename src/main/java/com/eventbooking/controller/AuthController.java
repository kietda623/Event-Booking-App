package com.eventbooking.controller;

import com.eventbooking.dto.auth.AuthResponse;
import com.eventbooking.dto.auth.LoginRequest;
import com.eventbooking.dto.auth.RegisterRequest;
import com.eventbooking.dto.common.ApiResponse;
import com.eventbooking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Registration successful")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Registration successful", authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }
}
