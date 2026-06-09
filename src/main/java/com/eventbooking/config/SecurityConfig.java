package com.eventbooking.config;

import com.eventbooking.dto.common.ApiResponse;
import com.eventbooking.security.CookieAuthFilter;
import com.eventbooking.security.JwtAuthenticationFilter;
import com.eventbooking.security.RateLimitingFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final CookieAuthFilter cookieAuthFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            CookieAuthFilter cookieAuthFilter,
            RateLimitingFilter rateLimitingFilter,
            ObjectMapper objectMapper
    ) {
        this.jwtFilter = jwtFilter;
        this.cookieAuthFilter = cookieAuthFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(),
                                    ApiResponse.error("UNAUTHENTICATED", "Unauthorized"));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(),
                                    ApiResponse.error("FORBIDDEN", "Forbidden"));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/metrics").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/push/vapid-public-key").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/*/bookings").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/tickets/checkin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/events/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/events/*/seats/hold").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/events/*/seats/hold").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/events/*/tiers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/events/*/tiers/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/*/tiers/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/events/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(cookieAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
