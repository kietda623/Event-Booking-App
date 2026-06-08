package com.eventbooking.payment;

import java.util.Map;

public record StripeWebhookEvent(
        String type,
        String paymentIntentId,
        Map<String, String> metadata
) {
}
