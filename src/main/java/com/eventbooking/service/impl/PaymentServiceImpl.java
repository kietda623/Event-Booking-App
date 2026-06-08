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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public PaymentResponse pay(PaymentRequest request) {
        Booking booking = findOwnedBooking(request.getBookingId());
        if ("PAID".equals(booking.getStatus()) || paymentRepository.existsByBookingIdAndStatus(booking.getId(), "PAID")) {
            throw new BusinessException("BOOKING_ALREADY_PAID", "Booking is already paid", HttpStatus.CONFLICT);
        }
        if (!"PENDING".equals(booking.getStatus())) {
            throw new BusinessException("BOOKING_NOT_PENDING", "Only pending bookings can be paid", HttpStatus.CONFLICT);
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setMethod(request.getMethod());
        payment.setStatus("PAID");
        payment.setPaymentDate(LocalDateTime.now());

        String oldStatus = booking.getStatus();
        booking.setStatus("PAID");
        bookingRepository.save(booking);
        logStatusTransition(booking, oldStatus, booking.getStatus());
        Payment saved = paymentRepository.save(payment);

        Ticket ticket = new Ticket();
        ticket.setBooking(booking);
        ticket.setUser(booking.getUser());
        ticket.setTicketCode(UUID.randomUUID().toString());
        ticket.setTicketType("GENERAL");
        ticket.setStatus("ACTIVE");
        ticket.setCheckedIn(false);
        Ticket savedTicket = ticketRepository.save(ticket);

        return toResponse(saved, savedTicket);
    }

    @Override
    public PaymentResponse getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PAYMENT_NOT_FOUND", "Payment not found"));
        Booking booking = payment.getBooking();
        if (!currentEmail().equalsIgnoreCase(booking.getUser().getEmail())) {
            throw new BusinessException("BOOKING_NOT_OWNED", "Booking is not owned by current user", HttpStatus.FORBIDDEN);
        }
        Ticket ticket = ticketRepository.findFirstByBookingIdOrderByIdDesc(booking.getId()).orElse(null);
        return toResponse(payment, ticket);
    }

    private Booking findOwnedBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND", "Booking not found"));
        if (!currentEmail().equalsIgnoreCase(booking.getUser().getEmail())) {
            throw new BusinessException("BOOKING_NOT_OWNED", "Booking is not owned by current user", HttpStatus.FORBIDDEN);
        }
        return booking;
    }

    private PaymentResponse toResponse(Payment payment, Ticket ticket) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getAmount(),
                payment.getStatus(),
                ticket == null ? null : ticket.getId(),
                ticket == null ? null : ticket.getTicketCode()
        );
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private void logStatusTransition(Booking booking, String oldStatus, String newStatus) {
        log.info("booking_status_transition bookingId={} userId={} oldStatus={} newStatus={} timestamp={}",
                booking.getId(),
                booking.getUser().getId(),
                oldStatus,
                newStatus,
                LocalDateTime.now());
    }
}
