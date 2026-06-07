package com.eventbooking.service.impl;

import com.eventbooking.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Override
    public void pay(Long bookingId) {
        // Payment processing is stubbed for runtime validation.
    }
}
