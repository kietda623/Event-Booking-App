package com.eventbooking.service.impl;

import com.eventbooking.dto.booking.BookingRequest;
import com.eventbooking.dto.booking.BookingResponse;
import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.Refund;
import com.eventbooking.entity.Ticket;
import com.eventbooking.entity.User;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.RefundRepository;
import com.eventbooking.repository.TicketRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RefundRepository refundRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public BookingResponse book(BookingRequest request) {
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantity < 1) {
            throw new BusinessException("INVALID_QUANTITY", "Quantity must be at least 1", HttpStatus.BAD_REQUEST);
        }

        Event event = eventRepository.findByIdForBooking(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("EVENT_NOT_FOUND", "Event not found"));
        int totalTickets = event.getTotalTickets() != null ? event.getTotalTickets() : 0;
        int booked = bookingRepository.sumBookedQuantityByEventId(event.getId()).intValue();
        if (quantity > Math.max(totalTickets - booked, 0)) {
            throw new BusinessException("EVENT_SOLD_OUT", "Not enough tickets available", HttpStatus.CONFLICT);
        }

        Booking booking = new Booking();
        booking.setEvent(event);
        booking.setUser(currentUser());
        booking.setQuantity(quantity);
        booking.setBookingDate(LocalDateTime.now());
        booking.setTotalPrice((event.getTicketPrice() != null ? event.getTicketPrice() : 0.0) * quantity);
        booking.setStatus("PENDING");

        return toResponse(bookingRepository.save(booking));
    }

    @Override
    public PageResponse<BookingResponse> myBookings(int page, int size) {
        Page<BookingResponse> bookings = bookingRepository.findByUserEmailOrderByBookingDateDesc(
                currentEmail(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        ).map(this::toResponse);
        return toPageResponse(bookings);
    }

    @Override
    @Transactional
    public BookingResponse cancel(Long id) {
        Booking booking = findOwnedBooking(id);
        if ("CANCELLED".equals(booking.getStatus()) || "COMPLETED".equals(booking.getStatus())) {
            throw new BusinessException("BOOKING_NOT_CANCELLABLE", "Booking cannot be cancelled", HttpStatus.CONFLICT);
        }
        if ("PENDING".equals(booking.getStatus())) {
            booking.setStatus("CANCELLED");
            return toResponse(bookingRepository.save(booking));
        }
        if ("PAID".equals(booking.getStatus())) {
            booking.setStatus("CANCELLED");
            Booking saved = bookingRepository.save(booking);

            Refund refund = new Refund();
            refund.setBookingId(saved.getId());
            refund.setAmount(saved.getTotalPrice());
            refund.setStatus("PENDING");
            refundRepository.save(refund);

            var tickets = ticketRepository.findByBookingId(saved.getId());
            for (Ticket ticket : tickets) {
                ticket.setStatus("CANCELLED");
            }
            ticketRepository.saveAll(tickets);
            return toResponse(saved);
        }
        throw new BusinessException("BOOKING_NOT_CANCELLABLE", "Booking cannot be cancelled", HttpStatus.CONFLICT);
    }

    @Override
    @Transactional
    public BookingResponse cancelPending(Long id) {
        Booking booking = findOwnedBooking(id);
        if (!"PENDING".equals(booking.getStatus())) {
            throw new BusinessException("BOOKING_NOT_PENDING", "Only pending bookings can be cancelled", HttpStatus.CONFLICT);
        }
        booking.setStatus("CANCELLED");
        return toResponse(bookingRepository.save(booking));
    }

    public Booking findOwnedBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND", "Booking not found"));
        if (!currentEmail().equalsIgnoreCase(booking.getUser().getEmail())) {
            throw new BusinessException("BOOKING_NOT_OWNED", "Booking is not owned by current user", HttpStatus.FORBIDDEN);
        }
        return booking;
    }

    private User currentUser() {
        return userRepository.findByEmail(currentEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getEvent().getId(),
                booking.getEvent().getTitle(),
                booking.getQuantity(),
                booking.getTotalPrice(),
                booking.getBookingDate(),
                booking.getStatus(),
                refundRepository.findFirstByBookingIdOrderByIdDesc(booking.getId())
                        .map(Refund::getStatus)
                        .orElse(null)
        );
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
