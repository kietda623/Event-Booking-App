package com.eventbooking.service.impl;

import com.eventbooking.dto.auth.AuthResponse;
import com.eventbooking.dto.auth.LoginRequest;
import com.eventbooking.dto.auth.RegisterRequest;
import com.eventbooking.entity.RefreshToken;
import com.eventbooking.entity.Role;
import com.eventbooking.entity.User;
import com.eventbooking.dto.auth.AuthResponse.AuthUserResponse;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.repository.RefreshTokenRepository;
import com.eventbooking.repository.RoleRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.security.AuthAttemptLimiter;
import com.eventbooking.security.JwtService;
import com.eventbooking.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthAttemptLimiter authAttemptLimiter;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        authAttemptLimiter.assertAllowed(email);
        if (userRepository.existsByEmail(email)) {
            authAttemptLimiter.recordFailure(email);
            logAuthFailure(email, "email_exists");
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
        authAttemptLimiter.reset(email);

        return toAuthResponse(saved);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        authAttemptLimiter.assertAllowed(email);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            authAttemptLimiter.recordFailure(email);
            logAuthFailure(email, "user_not_found");
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            authAttemptLimiter.recordFailure(email);
            logAuthFailure(email, "bad_password");
            throw invalidCredentials();
        }

        authAttemptLimiter.reset(email);
        return toAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken token = findUsableRefreshToken(refreshToken);
        User user = token.getUser();
        String accessToken = jwtService.generateToken(user.getEmail());
        return new AuthResponse(
                accessToken,
                jwtService.expiresAtFromNow().toString(),
                null,
                new AuthUserResponse(user.getId(), user.getFullName(), user.getEmail(), roleName(user))
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(refreshToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail());
        String refreshToken = createRefreshToken(user);
        return new AuthResponse(
                token,
                jwtService.expiresAtFromNow().toString(),
                refreshToken,
                new AuthUserResponse(user.getId(), user.getFullName(), user.getEmail(), roleName(user))
        );
    }

    private String createRefreshToken(User user) {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private RefreshToken findUsableRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessException("REFRESH_TOKEN_INVALID", "Refresh token is invalid", HttpStatus.UNAUTHORIZED);
        }
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessException("REFRESH_TOKEN_INVALID", "Refresh token is invalid", HttpStatus.UNAUTHORIZED));
        if (Boolean.TRUE.equals(refreshToken.getRevoked())
                || refreshToken.getExpiresAt() == null
                || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("REFRESH_TOKEN_INVALID", "Refresh token is invalid", HttpStatus.UNAUTHORIZED);
        }
        return refreshToken;
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
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

    private void logAuthFailure(String email, String reason) {
        log.warn("auth_failure email={} ip={} reason={}", email, authAttemptLimiter.currentIp(), reason);
    }
}
