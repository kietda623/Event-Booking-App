package com.eventbooking.controller;

import com.eventbooking.dto.common.ApiResponse;
import com.eventbooking.dto.payment.PaymentRequest;
import com.eventbooking.dto.payment.PaymentResponse;
import com.eventbooking.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Pay a pending booking with mock payment")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment completed successfully")
    public ApiResponse<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request){
        return ApiResponse.success("Payment completed successfully", paymentService.pay(request));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Handle Stripe payment webhooks")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    public ApiResponse<Void> webhook(
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signatureHeader
    ) {
        paymentService.handleWebhook(payload, signatureHeader);
        return ApiResponse.success("Webhook processed", null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment detail")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment retrieved successfully")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable Long id) {
        return ApiResponse.success("Payment retrieved successfully", paymentService.getPayment(id));
    }
}
