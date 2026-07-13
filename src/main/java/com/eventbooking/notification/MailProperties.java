package com.eventbooking.notification;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {
    private boolean enabled;
    private String from = "noreply@example.com";
}
