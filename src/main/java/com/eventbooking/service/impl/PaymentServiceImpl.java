package com.eventbooking.service.impl;

import com.eventbooking.dto.payment.PaymentRequest;
import com.eventbooking.dto.payment.PaymentResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Payment;
import com.eventbooking.entity.Ticket;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.PaymentRepository;
import com.eventbooking.repository.TicketRepository;
import com.eventbooking.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public PaymentResponse pay(PaymentRequest request) {
        Booking booking = bookingRepository.findByIdAndUserUsername(request.getBookingId(), currentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if ("PAID".equals(booking.getStatus()) || paymentRepository.existsByBookingIdAndStatus(booking.getId(), "PAID")) {
            throw new BusinessException("Booking is already paid");
        }
        if ("CANCELLED".equals(booking.getStatus())) {
            throw new BusinessException("Cancelled bookings cannot be paid");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setMethod(request.getMethod() != null ? request.getMethod() : "MOCK_CARD");
        payment.setStatus("PAID");
        payment.setPaymentDate(LocalDateTime.now());

        booking.setStatus("PAID");
        bookingRepository.save(booking);
        Payment saved = paymentRepository.save(payment);

        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setTicketCode("TICKET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ticketRepository.save(ticket);

        return new PaymentResponse(saved.getId(), booking.getId(), saved.getAmount(), saved.getStatus());
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
