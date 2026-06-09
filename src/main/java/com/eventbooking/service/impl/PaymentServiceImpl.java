package com.eventbooking.service.impl;

import com.eventbooking.dto.payment.PaymentRequest;
import com.eventbooking.dto.payment.PaymentResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Payment;
import com.eventbooking.entity.Ticket;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.notification.NotificationService;
import com.eventbooking.payment.StripePaymentClient;
import com.eventbooking.payment.StripePaymentIntentRequest;
import com.eventbooking.payment.StripePaymentIntentResult;
import com.eventbooking.payment.StripeWebhookEvent;
import com.eventbooking.payment.StripeWebhookVerifier;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.PaymentRepository;
import com.eventbooking.repository.TicketRepository;
import com.eventbooking.service.PaymentService;
import com.eventbooking.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final StripePaymentClient stripePaymentClient;
    private final StripeWebhookVerifier stripeWebhookVerifier;
    private final NotificationService notificationService;
    private final Environment environment;
    private final SeatService seatService;

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

        String method = request.getMethod().trim().toUpperCase();
        if ("STRIPE".equals(method)) {
            return createStripePaymentIntent(booking);
        }
        if (!isMockMethod(method) || !isMockAllowed()) {
            throw new BusinessException("PAYMENT_METHOD_UNSUPPORTED", "Payment method is not supported", HttpStatus.BAD_REQUEST);
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setMethod(method);
        payment.setStatus("PAID");
        payment.setPaymentDate(LocalDateTime.now());

        return completePaidBooking(booking, payment, true);
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        StripeWebhookEvent event = stripeWebhookVerifier.verify(payload, signatureHeader);
        if (event == null || event.type() == null) {
            return;
        }
        switch (event.type()) {
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
            default -> log.debug("stripe_webhook_ignored type={}", event.type());
        }
    }

    private PaymentResponse createStripePaymentIntent(Booking booking) {
        var existing = paymentRepository.findFirstByBookingIdAndMethodOrderByIdDesc(booking.getId(), "STRIPE")
                .filter(payment -> !"FAILED".equals(payment.getStatus()))
                .filter(payment -> payment.getClientSecret() != null && !payment.getClientSecret().isBlank());
        if (existing.isPresent()) {
            return toResponse(existing.get(), null);
        }

        long amount = Math.round((booking.getTotalPrice() != null ? booking.getTotalPrice() : 0.0) * 100);
        StripePaymentIntentResult intent = stripePaymentClient.createPaymentIntent(
                new StripePaymentIntentRequest(amount, "vnd", Map.of("bookingId", String.valueOf(booking.getId())))
        );

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setMethod("STRIPE");
        payment.setStatus("PENDING");
        payment.setPaymentIntentId(intent.paymentIntentId());
        payment.setClientSecret(intent.clientSecret());
        payment.setPaymentDate(LocalDateTime.now());
        return toResponse(paymentRepository.save(payment), null);
    }

    private void handlePaymentIntentSucceeded(StripeWebhookEvent event) {
        Booking booking = findBookingFromWebhook(event);
        Payment payment = paymentRepository.findByPaymentIntentId(event.paymentIntentId())
                .orElseGet(Payment::new);
        if ("PAID".equals(booking.getStatus()) || "PAID".equals(payment.getStatus())) {
            return;
        }
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        payment.setMethod("STRIPE");
        payment.setStatus("PAID");
        payment.setPaymentIntentId(event.paymentIntentId());
        payment.setPaymentDate(LocalDateTime.now());
        completePaidBooking(booking, payment, true);
    }

    private void handlePaymentIntentFailed(StripeWebhookEvent event) {
        Booking booking = findBookingFromWebhook(event);
        if (!"PAID".equals(booking.getStatus()) && !"CANCELLED".equals(booking.getStatus())) {
            booking.setStatus("PENDING");
            bookingRepository.save(booking);
        }
        paymentRepository.findByPaymentIntentId(event.paymentIntentId()).ifPresent(payment -> {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
        });
        log.warn("stripe_payment_failed bookingId={} paymentIntentId={}", booking.getId(), event.paymentIntentId());
    }

    private Booking findBookingFromWebhook(StripeWebhookEvent event) {
        String bookingId = event.metadata() == null ? null : event.metadata().get("bookingId");
        if (bookingId == null || bookingId.isBlank()) {
            throw new BusinessException("STRIPE_WEBHOOK_INVALID", "Stripe webhook is missing bookingId metadata", HttpStatus.BAD_REQUEST);
        }
        return bookingRepository.findById(Long.valueOf(bookingId))
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND", "Booking not found"));
    }

    private PaymentResponse completePaidBooking(Booking booking, Payment payment, boolean notify) {
        String oldStatus = booking.getStatus();
        booking.setStatus("PAID");
        bookingRepository.save(booking);
        seatService.bookSeats(booking);
        logStatusTransition(booking, oldStatus, booking.getStatus());
        Payment saved = paymentRepository.save(payment);

        Ticket savedTicket = createTicketsIfNeeded(booking);
        if (notify) {
            notificationService.sendBookingPaidEmail(booking, savedTicket);
        }

        return toResponse(saved, savedTicket);
    }

    private Ticket createTicketsIfNeeded(Booking booking) {
        List<Ticket> existingTickets = ticketRepository.findByBookingId(booking.getId());
        if (!existingTickets.isEmpty()) {
            return existingTickets.get(0);
        }
        List<String> seats = splitSeatNumbers(booking.getSeatNumbers());
        int quantity = booking.getQuantity() != null ? booking.getQuantity() : 1;
        int ticketCount = seats.isEmpty() ? Math.max(quantity, 1) : seats.size();
        List<Ticket> tickets = new ArrayList<>();
        for (int index = 0; index < ticketCount; index++) {
            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setUser(booking.getUser());
            ticket.setTier(booking.getTier());
            ticket.setTicketCode(UUID.randomUUID().toString());
            ticket.setTicketType(booking.getTier() == null ? "GENERAL" : booking.getTier().getName());
            ticket.setSeatNumber(seats.isEmpty() ? null : seats.get(index));
            ticket.setStatus("ACTIVE");
            ticket.setCheckedIn(false);
            tickets.add(ticket);
        }
        return ticketRepository.saveAll(tickets).get(0);
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
                ticket == null ? null : ticket.getTicketCode(),
                payment.getClientSecret(),
                payment.getPaymentIntentId()
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

    private boolean isMockMethod(String method) {
        return "MOCK".equals(method)
                || "MOCK_CARD".equals(method)
                || "CREDIT_CARD".equals(method)
                || "BANK_TRANSFER".equals(method)
                || "E_WALLET".equals(method);
    }

    private boolean isMockAllowed() {
        return !Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private List<String> splitSeatNumbers(String seatNumbers) {
        if (seatNumbers == null || seatNumbers.isBlank()) {
            return List.of();
        }
        return Arrays.stream(seatNumbers.split(","))
                .filter(seatNumber -> !seatNumber.isBlank())
                .map(String::trim)
                .toList();
    }
}
