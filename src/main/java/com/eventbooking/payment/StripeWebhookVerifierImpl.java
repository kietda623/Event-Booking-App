package com.eventbooking.payment;

import com.eventbooking.exception.BusinessException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StripeWebhookVerifierImpl implements StripeWebhookVerifier {
    private final String webhookSecret;

    public StripeWebhookVerifierImpl(@Value("${app.stripe.webhook-secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    public StripeWebhookEvent verify(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new BusinessException("STRIPE_WEBHOOK_NOT_CONFIGURED", "Stripe webhook secret is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            com.stripe.model.Event event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Object stripeObject = deserializer.getObject()
                    .orElseThrow(() -> new BusinessException("STRIPE_WEBHOOK_INVALID", "Stripe webhook payload is invalid", HttpStatus.BAD_REQUEST));
            if (stripeObject instanceof PaymentIntent intent) {
                Map<String, String> metadata = intent.getMetadata();
                return new StripeWebhookEvent(event.getType(), intent.getId(), metadata);
            }
            return new StripeWebhookEvent(event.getType(), null, Map.of());
        } catch (SignatureVerificationException ex) {
            throw new BusinessException("STRIPE_SIGNATURE_INVALID", "Stripe webhook signature is invalid", HttpStatus.UNAUTHORIZED);
        }
    }
}
