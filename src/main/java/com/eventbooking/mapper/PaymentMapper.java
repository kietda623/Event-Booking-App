package com.eventbooking.mapper;

import com.eventbooking.dto.payment.PaymentResponse;
import com.eventbooking.entity.Payment;
import com.eventbooking.entity.Ticket;

public final class PaymentMapper {
    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment, Ticket ticket) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getAmount(),
                payment.getStatus(),
                ticket == null ? null : ticket.getId(),
                ticket == null ? null : ticket.getTicketCode(),
                payment.getClientSecret(),
                payment.getPaymentIntentId()
        );
    }
}
