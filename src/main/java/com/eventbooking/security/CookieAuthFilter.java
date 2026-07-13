package com.eventbooking.security;

import com.eventbooking.entity.User;
import com.eventbooking.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CookieAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthCookieService authCookieService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        boolean hasBearerHeader = request.getHeader("Authorization") != null;
        if (!hasBearerHeader && SecurityContextHolder.getContext().getAuthentication() == null) {
            authCookieService.readCookie(request, AuthCookieService.ACCESS_COOKIE)
                    .ifPresent(token -> authenticate(token, request));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            String email = jwtService.extractSubject(token);
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null && jwtService.isValid(token, email)) {
                var authorities = user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                        .toList();
                var authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
