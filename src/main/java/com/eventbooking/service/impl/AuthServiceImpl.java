package com.eventbooking.service.impl;

import com.eventbooking.dto.auth.AuthResponse;
import com.eventbooking.dto.auth.LoginRequest;
import com.eventbooking.dto.auth.RegisterRequest;
import com.eventbooking.security.JwtService;
import com.eventbooking.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        String token = jwtService.generateToken(request.getUsername());
        return new AuthResponse(token, request.getUsername());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String token = jwtService.generateToken(request.getUsername());
        return new AuthResponse(token, request.getUsername());
    }
}