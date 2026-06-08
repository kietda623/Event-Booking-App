package com.eventbooking.payment;

import com.eventbooking.exception.BusinessException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class StripePaymentClientImpl implements StripePaymentClient {
    private final String secretKey;

    public StripePaymentClientImpl(@Value("${app.stripe.secret-key:}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public StripePaymentIntentResult createPaymentIntent(StripePaymentIntentRequest request) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new BusinessException("STRIPE_NOT_CONFIGURED", "Stripe secret key is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Stripe.apiKey = secretKey;
        PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(request.amount())
                .setCurrency(request.currency());
        request.metadata().forEach(builder::putMetadata);
        try {
            PaymentIntent intent = PaymentIntent.create(builder.build());
            return new StripePaymentIntentResult(intent.getId(), intent.getClientSecret());
        } catch (StripeException ex) {
            throw new BusinessException("PAYMENT_GATEWAY_ERROR", "Could not create Stripe payment intent", HttpStatus.BAD_GATEWAY);
        }
    }
}
