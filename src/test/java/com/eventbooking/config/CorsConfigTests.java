package com.eventbooking.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorsConfigTests {
    @Test
    void stagingProfileWithoutCorsOriginsFailsFast() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});

        CorsConfig config = new CorsConfig(environment);
        ReflectionTestUtils.setField(config, "allowedOrigins", "");

        assertThatThrownBy(config::validateProdCors)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS_ALLOWED_ORIGINS");
    }
}
