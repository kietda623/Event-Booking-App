package com.eventbooking.payment;

public interface StripeWebhookVerifier {
    StripeWebhookEvent verify(String payload, String signatureHeader);
}
