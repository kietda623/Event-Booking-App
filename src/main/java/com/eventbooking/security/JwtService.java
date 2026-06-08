package com.eventbooking.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiry-minutes:1440}")
    private long expiryMinutes;

    public String generateToken(String subject) {
        Date now = new Date();
        Date expiresAt = Date.from(now.toInstant().plus(accessTokenDuration()));

        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey())
                .compact();
    }

    public String generateToken(Object payload) {
        return generateToken(String.valueOf(payload));
    }

    public String extractUsername(String token) {
        return extractSubject(token);
    }

    public String extractSubject(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isValid(String token, String subject) {
        return subject.equals(extractSubject(token));
    }

    public Instant expiresAtFromNow() {
        return Instant.now().plus(accessTokenDuration());
    }

    public Duration accessTokenDuration() {
        return Duration.ofMinutes(expiryMinutes);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
