package com.eventbooking.payment;

public interface StripePaymentClient {
    StripePaymentIntentResult createPaymentIntent(StripePaymentIntentRequest request);
}
