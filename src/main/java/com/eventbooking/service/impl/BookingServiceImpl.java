package com.eventbooking.service.impl;

import com.eventbooking.dto.booking.BookingRequest;
import com.eventbooking.dto.booking.BookingResponse;
import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.Refund;
import com.eventbooking.entity.Ticket;
import com.eventbooking.entity.TicketTier;
import com.eventbooking.entity.User;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.mapper.BookingMapper;
import com.eventbooking.notification.NotificationService;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.RefundRepository;
import com.eventbooking.repository.TicketTierRepository;
import com.eventbooking.repository.TicketRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.service.BookingService;
import com.eventbooking.service.SeatService;
import com.eventbooking.util.PageResponseMapper;
import com.eventbooking.util.SeatNumberUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);
    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RefundRepository refundRepository;
    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;
    private final TicketTierRepository ticketTierRepository;
    private final SeatService seatService;
    private final BookingMapper bookingMapper;

    @Override
    public BookingResponse book(BookingRequest request) {
        int quantity = request.getQuantity() != null ? request.getQuantity() : 1;
        if (quantity < 1) {
            throw new BusinessException("INVALID_QUANTITY", "Quantity must be at least 1", HttpStatus.BAD_REQUEST);
        }

        Event event = eventRepository.findByIdForBooking(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("EVENT_NOT_FOUND", "Event not found"));

        TicketTier tier = ticketTierRepository.findByIdForBooking(request.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("TIER_NOT_FOUND", "Ticket tier not found"));
        if (!tier.getEvent().getId().equals(event.getId())) {
            throw new ResourceNotFoundException("TIER_NOT_FOUND", "Ticket tier not found");
        }
        int totalTickets = tier.getTotalQuantity() != null ? tier.getTotalQuantity() : 0;
        int soldTickets = tier.getSoldQuantity() != null ? tier.getSoldQuantity() : 0;
        if (quantity > Math.max(totalTickets - soldTickets, 0)) {
            throw new BusinessException("TIER_SOLD_OUT", "Ticket tier is sold out", HttpStatus.CONFLICT);
        }

        List<String> seatNumbers = SeatNumberUtils.normalize(request.getSeatNumbers());
        if (!seatNumbers.isEmpty() && seatNumbers.size() != quantity) {
            throw new BusinessException("INVALID_SEATS", "Seat count must match booking quantity", HttpStatus.BAD_REQUEST);
        }
        User user = currentUser();
        seatService.validateHeldSeats(event, tier, user, seatNumbers);

        tier.setSoldQuantity(soldTickets + quantity);
        ticketTierRepository.save(tier);

        Booking booking = new Booking();
        booking.setEvent(event);
        booking.setUser(user);
        booking.setTier(tier);
        booking.setQuantity(quantity);
        booking.setBookingDate(LocalDateTime.now());
        booking.setTotalPrice((tier.getPrice() != null ? tier.getPrice() : 0.0) * quantity);
        booking.setStatus("PENDING");
        booking.setSeatNumbers(String.join(",", seatNumbers));

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Override
    public PageResponse<BookingResponse> myBookings(int page, int size) {
        Page<BookingResponse> bookings = bookingRepository.findByUserEmailOrderByBookingDateDesc(
                currentEmail(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        ).map(bookingMapper::toResponse);
        return PageResponseMapper.from(bookings);
    }

    @Override
    public BookingResponse cancel(Long id) {
        Booking booking = findOwnedBooking(id);
        if ("CANCELLED".equals(booking.getStatus()) || "COMPLETED".equals(booking.getStatus())) {
            throw new BusinessException("BOOKING_NOT_CANCELLABLE", "Booking cannot be cancelled", HttpStatus.CONFLICT);
        }
        if ("PENDING".equals(booking.getStatus())) {
            String oldStatus = booking.getStatus();
            booking.setStatus("CANCELLED");
            Booking saved = bookingRepository.save(booking);
            releaseInventory(saved);
            logStatusTransition(saved, oldStatus, saved.getStatus());
            return bookingMapper.toResponse(saved);
        }
        if ("PAID".equals(booking.getStatus())) {
            String oldStatus = booking.getStatus();
            booking.setStatus("CANCELLED");
            Booking saved = bookingRepository.save(booking);
            releaseInventory(saved);
            logStatusTransition(saved, oldStatus, saved.getStatus());

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
            notificationService.sendBookingCancelledEmail(saved);
            return bookingMapper.toResponse(saved);
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
        String oldStatus = booking.getStatus();
        booking.setStatus("CANCELLED");
        Booking saved = bookingRepository.save(booking);
        releaseInventory(saved);
        logStatusTransition(saved, oldStatus, saved.getStatus());
        return bookingMapper.toResponse(saved);
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

    private void releaseInventory(Booking booking) {
        if (booking.getTier() != null) {
            TicketTier tier = ticketTierRepository.findByIdForBooking(booking.getTier().getId())
                    .orElse(null);
            if (tier != null) {
                int sold = tier.getSoldQuantity() != null ? tier.getSoldQuantity() : 0;
                int quantity = booking.getQuantity() != null ? booking.getQuantity() : 0;
                tier.setSoldQuantity(Math.max(sold - quantity, 0));
                ticketTierRepository.save(tier);
            }
        }
        seatService.releaseBookingSeats(booking);
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
