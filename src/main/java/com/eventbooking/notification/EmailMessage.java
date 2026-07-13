package com.eventbooking.notification;

public record EmailMessage(
        String to,
        String subject,
        String html
) {
}
