package com.eventbooking.payment;

import java.util.Map;

public record StripePaymentIntentRequest(
        long amount,
        String currency,
        Map<String, String> metadata
) {
}
