package com.eventbooking.service.impl;

import com.eventbooking.dto.booking.BookingRequest;
import com.eventbooking.dto.booking.BookingResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.User;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingResponse book(BookingRequest request) {
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantity < 1) {
            throw new BusinessException("Quantity must be at least 1");
        }

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        int totalTickets = event.getTotalTickets() != null ? event.getTotalTickets() : 0;
        int booked = bookingRepository.sumBookedQuantityByEventId(event.getId()).intValue();
        if (quantity > Math.max(totalTickets - booked, 0)) {
            throw new BusinessException("Not enough tickets available");
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
    public List<BookingResponse> myBookings() {
        return bookingRepository.findByUserUsernameOrderByBookingDateDesc(currentUsername()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse cancel(Long id) {
        Booking booking = bookingRepository.findByIdAndUserUsername(id, currentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (!"PENDING".equals(booking.getStatus())) {
            throw new BusinessException("Only pending bookings can be cancelled");
        }
        booking.setStatus("CANCELLED");
        return toResponse(bookingRepository.save(booking));
    }

    private User currentUser() {
        return userRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String currentUsername() {
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
                booking.getStatus()
        );
    }
}
