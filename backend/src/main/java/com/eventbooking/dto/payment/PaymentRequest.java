package com.eventbooking.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotNull
    private Long bookingId;

    private String cardNumber;

    private String expiry;

    private String cvv;

    private String method;
}
