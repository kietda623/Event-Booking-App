package com.eventbooking.service.impl;

import com.eventbooking.dto.auth.AuthResponse;
import com.eventbooking.dto.auth.LoginRequest;
import com.eventbooking.dto.auth.RegisterRequest;
import com.eventbooking.entity.Role;
import com.eventbooking.entity.User;
import com.eventbooking.dto.auth.AuthResponse.AuthUserResponse;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.repository.RoleRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.JwtService;
import com.eventbooking.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email already exists", HttpStatus.CONFLICT);
        }

        Role role = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER")));

        User user = new User();
        user.setUsername(email);
        user.setFullName(request.getFullName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRoles(new HashSet<>(Set.of(role)));
        User saved = userRepository.save(user);

        return toAuthResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw invalidCredentials();
        }

        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(
                token,
                jwtService.expiresAtFromNow().toString(),
                new AuthUserResponse(user.getId(), user.getFullName(), user.getEmail(), roleName(user))
        );
    }

    private String roleName(User user) {
        return user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("USER");
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS", "Invalid credentials", HttpStatus.UNAUTHORIZED);
    }
}
