package com.eventbooking.security;

import com.eventbooking.dto.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final InMemoryRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(InMemoryRateLimiter rateLimiter, ClientIpResolver clientIpResolver, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod()) || shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String key;
        int limit;
        if (path.startsWith("/api/bookings") || path.startsWith("/api/payments")) {
            key = "user-api:" + currentUserOrIp(request);
            limit = 20;
        } else if (path.startsWith("/api/")) {
            key = "ip-api:" + clientIpResolver.resolve(request);
            limit = 200;
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        if (!rateLimiter.tryAcquire(key, limit, WINDOW)) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.error("TOO_MANY_REQUESTS", "Too many requests"));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.startsWith("/actuator/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    private String currentUserOrIp(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return authentication.getName();
        }
        return clientIpResolver.resolve(request);
    }
}
