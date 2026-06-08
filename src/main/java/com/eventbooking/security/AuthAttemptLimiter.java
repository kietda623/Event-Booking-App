package com.eventbooking.security;

import com.eventbooking.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Component
public class AuthAttemptLimiter {
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final InMemoryRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public AuthAttemptLimiter(InMemoryRateLimiter rateLimiter, ClientIpResolver clientIpResolver) {
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    public void assertAllowed(String email) {
        if (rateLimiter.isAtLimit(key(email), MAX_FAILED_ATTEMPTS, WINDOW)) {
            throw new BusinessException("TOO_MANY_REQUESTS", "Too many failed attempts", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public void recordFailure(String email) {
        rateLimiter.record(key(email), WINDOW);
    }

    public void reset(String email) {
        rateLimiter.reset(key(email));
    }

    public String currentIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        return request == null ? "unknown" : clientIpResolver.resolve(request);
    }

    private String key(String email) {
        return "auth:" + normalize(email) + ":" + currentIp();
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
