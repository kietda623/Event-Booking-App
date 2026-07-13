package com.eventbooking.config;

import com.eventbooking.entity.Role;
import com.eventbooking.entity.User;
import com.eventbooking.repository.RoleRepository;
import com.eventbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${app.admin.seed:false}")
    private boolean seedAdmin;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.email:admin@example.com}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        Set<String> activeProfiles = Set.of(environment.getActiveProfiles());
        if (seedAdmin && activeProfiles.contains("prod")) {
            throw new IllegalStateException("app.admin.seed cannot be true when prod profile is active");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER")));
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN")));

        boolean seedProfile = activeProfiles.contains("dev") || activeProfiles.contains("test");
        if (!seedAdmin || !seedProfile) {
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("app.admin.password must be set when admin seed is enabled");
        }
        String email = adminEmail.trim().toLowerCase();
        if (!userRepository.existsByEmail(email)) {
            User admin = new User();
            admin.setUsername(email);
            admin.setFullName("Admin");
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRoles(new HashSet<>(Set.of(userRole, adminRole)));
            userRepository.save(admin);
        }
    }
}
