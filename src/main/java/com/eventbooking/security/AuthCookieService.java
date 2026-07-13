package com.eventbooking.security;

import com.eventbooking.dto.auth.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthCookieService {
    public static final String ACCESS_COOKIE = "accessToken";
    public static final String REFRESH_COOKIE = "refreshToken";
    private static final Duration REFRESH_DURATION = Duration.ofDays(7);

    private final JwtService jwtService;
    private final boolean cookieSecure;

    public AuthCookieService(
            JwtService jwtService,
            @Value("${app.auth.cookie-secure:false}") boolean cookieSecure
    ) {
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
    }

    public void addAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        addCookie(response, ACCESS_COOKIE, authResponse.getAccessToken(), jwtService.accessTokenDuration());
        if (authResponse.getRefreshToken() != null && !authResponse.getRefreshToken().isBlank()) {
            addCookie(response, REFRESH_COOKIE, authResponse.getRefreshToken(), REFRESH_DURATION);
        }
    }

    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_COOKIE, "", Duration.ZERO);
        addCookie(response, REFRESH_COOKIE, "", Duration.ZERO);
    }

    public Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
