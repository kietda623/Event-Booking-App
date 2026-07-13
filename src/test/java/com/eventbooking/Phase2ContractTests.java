package com.eventbooking;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class Phase2ContractTests {
    @Test
    void pomUsesSpringdocVersionCompatibleWithSpringBoot35() throws Exception {
        String pom = Files.readString(Path.of("pom.xml"));

        assertThat(pom).contains("<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>");
        assertThat(pom).contains("<version>2.8.17</version>");
    }

    @Test
    void prodProfileDisablesSwaggerAndApiDocs() throws Exception {
        String prodProperties = Files.readString(Path.of("src/main/resources/application-prod.properties"));

        assertThat(prodProperties).contains("springdoc.api-docs.enabled=false");
        assertThat(prodProperties).contains("springdoc.swagger-ui.enabled=false");
    }

    @Test
    void controllersHaveOpenApiTagsAndEndpointOperations() {
        Class<?>[] controllers = {
                com.eventbooking.controller.AuthController.class,
                com.eventbooking.controller.EventController.class,
                com.eventbooking.controller.BookingController.class,
                com.eventbooking.controller.PaymentController.class,
                com.eventbooking.controller.TicketController.class,
                com.eventbooking.controller.UserController.class,
                com.eventbooking.controller.FavoriteController.class
        };

        for (Class<?> controller : controllers) {
            assertThat(hasAnnotation(controller.getAnnotations(), "Tag"))
                    .as(controller.getSimpleName() + " should have @Tag")
                    .isTrue();
            for (Method method : controller.getDeclaredMethods()) {
                if (Arrays.stream(method.getAnnotations())
                        .anyMatch(annotation -> annotation.annotationType().getSimpleName().endsWith("Mapping"))) {
                    assertThat(hasAnnotation(method.getAnnotations(), "Operation"))
                            .as(method + " should have @Operation")
                            .isTrue();
                    assertThat(hasAnnotation(method.getAnnotations(), "ApiResponse"))
                            .as(method + " should have @ApiResponse")
                            .isTrue();
                }
            }
        }
    }

    private boolean hasAnnotation(java.lang.annotation.Annotation[] annotations, String simpleName) {
        return Arrays.stream(annotations)
                .anyMatch(annotation -> annotation.annotationType().getSimpleName().equals(simpleName));
    }
}
