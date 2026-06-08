package com.eventbooking;

import com.eventbooking.dto.payment.PaymentRequest;
import com.eventbooking.entity.Event;
import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class Phase1ContractTests {
    @Test
    void paymentRequestDoesNotExposeRawCardFields() {
        assertThat(Arrays.stream(PaymentRequest.class.getDeclaredFields()).map(Field::getName))
                .containsExactlyInAnyOrder("bookingId", "method");
    }

    @Test
    void eventHasVersionFieldForOptimisticLocking() throws Exception {
        Field version = Event.class.getDeclaredField("version");

        assertThat(version.getType()).isEqualTo(Long.class);
        assertThat(version.getAnnotation(Version.class)).isNotNull();
    }

    @Test
    void mainConfigUsesEnvironmentPlaceholdersForSecrets() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application.properties"));

        assertThat(properties).contains("spring.datasource.password=${DB_PASSWORD}");
        assertThat(properties).contains("app.jwt.secret=${JWT_SECRET}");
        assertThat(properties).contains("app.admin.seed=${ADMIN_SEED:false}");
        assertThat(properties).doesNotContain("Dakvip24@");
        assertThat(Files.exists(Path.of(".env.example"))).isTrue();
    }
}
