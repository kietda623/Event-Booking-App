package com.eventbooking.payment;

public record StripePaymentIntentResult(
        String paymentIntentId,
        String clientSecret
) {
}
