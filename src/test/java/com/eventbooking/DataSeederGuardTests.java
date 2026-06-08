package com.eventbooking;

import com.eventbooking.config.DataSeeder;
import com.eventbooking.entity.Role;
import com.eventbooking.entity.User;
import com.eventbooking.repository.RoleRepository;
import com.eventbooking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSeederGuardTests {
    @Test
    void prodProfileWithAdminSeedEnabledFailsFast() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        DataSeeder seeder = new DataSeeder(roleRepository, userRepository, passwordEncoder, environment);
        ReflectionTestUtils.setField(seeder, "seedAdmin", true);

        assertThatThrownBy(() -> seeder.run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.admin.seed");
    }

    @Test
    void adminSeedDoesNotCreateUserOutsideDevOrTestProfiles() throws Exception {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role(1L, "USER")));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(new Role(2L, "ADMIN")));

        DataSeeder seeder = new DataSeeder(roleRepository, userRepository, passwordEncoder, environment);
        ReflectionTestUtils.setField(seeder, "seedAdmin", true);

        seeder.run();

        verify(userRepository, never()).save(any(User.class));
    }
}
