package com.eventbooking.service.impl;

import com.eventbooking.dto.seat.SeatHoldRequest;
import com.eventbooking.dto.seat.SeatResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.Seat;
import com.eventbooking.entity.TicketTier;
import com.eventbooking.entity.User;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.SeatRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {
    private static final int HOLD_MINUTES = 10;

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> list(Long eventId) {
        findEvent(eventId);
        return seatRepository.findByEventIdOrderByRowAscColAscIdAsc(eventId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<SeatResponse> hold(Long eventId, SeatHoldRequest request) {
        findEvent(eventId);
        User user = currentUser();
        List<String> requested = normalizeSeatNumbers(request.getSeatNumbers());
        List<Seat> seats = lockRequestedSeats(eventId, requested);
        LocalDateTime heldUntil = LocalDateTime.now().plusMinutes(HOLD_MINUTES);
        for (Seat seat : seats) {
            if (!"AVAILABLE".equals(seat.getStatus())) {
                throw seatNotAvailable();
            }
            seat.setStatus("HELD");
            seat.setHeldUntil(heldUntil);
            seat.setHeldByUserId(user.getId());
        }
        return seatRepository.saveAll(seats).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void releaseHeld(Long eventId) {
        findEvent(eventId);
        seatRepository.releaseHeldSeats(eventId, currentUser().getId());
    }

    @Override
    @Transactional
    public void validateHeldSeats(Event event, TicketTier tier, User user, List<String> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            return;
        }
        List<String> requested = normalizeSeatNumbers(seatNumbers);
        List<Seat> seats = lockRequestedSeats(event.getId(), requested);
        LocalDateTime now = LocalDateTime.now();
        for (Seat seat : seats) {
            boolean sameUser = seat.getHeldByUserId() != null && seat.getHeldByUserId().equals(user.getId());
            boolean sameTier = seat.getTier() != null && seat.getTier().getId().equals(tier.getId());
            boolean holdActive = seat.getHeldUntil() != null && seat.getHeldUntil().isAfter(now);
            if (!"HELD".equals(seat.getStatus()) || !sameUser || !sameTier || !holdActive) {
                throw seatNotAvailable();
            }
        }
    }

    @Override
    @Transactional
    public void bookSeats(Booking booking) {
        List<String> seatNumbers = splitSeatNumbers(booking.getSeatNumbers());
        if (seatNumbers.isEmpty()) {
            return;
        }
        List<Seat> seats = lockRequestedSeats(booking.getEvent().getId(), seatNumbers);
        for (Seat seat : seats) {
            seat.setStatus("BOOKED");
            seat.setHeldUntil(null);
            seat.setHeldByUserId(booking.getUser().getId());
        }
        seatRepository.saveAll(seats);
    }

    @Override
    @Transactional
    public void releaseBookingSeats(Booking booking) {
        List<String> seatNumbers = splitSeatNumbers(booking.getSeatNumbers());
        if (seatNumbers.isEmpty()) {
            return;
        }
        List<Seat> seats = lockRequestedSeats(booking.getEvent().getId(), seatNumbers);
        for (Seat seat : seats) {
            seat.setStatus("AVAILABLE");
            seat.setHeldUntil(null);
            seat.setHeldByUserId(null);
        }
        seatRepository.saveAll(seats);
    }

    private List<Seat> lockRequestedSeats(Long eventId, List<String> requested) {
        List<Seat> seats = seatRepository.findByEventIdAndSeatNumbersForUpdate(eventId, requested);
        if (seats.size() != requested.size()) {
            throw seatNotAvailable();
        }
        return seats;
    }

    private List<String> normalizeSeatNumbers(List<String> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new BusinessException("INVALID_SEATS", "At least one seat is required", HttpStatus.BAD_REQUEST);
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String seatNumber : seatNumbers) {
            if (seatNumber == null || seatNumber.isBlank()) {
                throw new BusinessException("INVALID_SEATS", "Seat numbers cannot be blank", HttpStatus.BAD_REQUEST);
            }
            unique.add(seatNumber.trim());
        }
        return new ArrayList<>(unique);
    }

    private List<String> splitSeatNumbers(String seatNumbers) {
        if (seatNumbers == null || seatNumbers.isBlank()) {
            return List.of();
        }
        return normalizeSeatNumbers(List.of(seatNumbers.split(",")));
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("EVENT_NOT_FOUND", "Event not found"));
    }

    private User currentUser() {
        return userRepository.findByEmail(currentEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private BusinessException seatNotAvailable() {
        return new BusinessException("SEAT_NOT_AVAILABLE", "Seat is not available", HttpStatus.CONFLICT);
    }

    private SeatResponse toResponse(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getEvent().getId(),
                seat.getTier() == null ? null : seat.getTier().getId(),
                seat.getSeatNumber(),
                seat.getRow(),
                seat.getCol(),
                seat.getStatus(),
                seat.getHeldUntil(),
                seat.getHeldByUserId()
        );
    }
}
