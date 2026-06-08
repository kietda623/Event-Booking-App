package com.eventbooking.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotNull
    private Long bookingId;

    @NotBlank
    @Pattern(regexp = "MOCK_CARD|CREDIT_CARD|BANK_TRANSFER|E_WALLET", message = "method must be MOCK_CARD, CREDIT_CARD, BANK_TRANSFER or E_WALLET")
    private String method;
}
