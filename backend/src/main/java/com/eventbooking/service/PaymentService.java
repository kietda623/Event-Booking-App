package com.eventbooking.service;

import com.eventbooking.dto.payment.PaymentRequest;
import com.eventbooking.dto.payment.PaymentResponse;

public interface PaymentService {
    PaymentResponse pay(PaymentRequest request);
}
