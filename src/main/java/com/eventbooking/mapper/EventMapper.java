package com.eventbooking.mapper;

import com.eventbooking.dto.booking.EventBookingResponse;
import com.eventbooking.dto.event.EventResponse;
import com.eventbooking.dto.tier.TicketTierResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.TicketTier;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventMapper {
    private final BookingRepository bookingRepository;
    private final TicketTierRepository ticketTierRepository;

    public EventResponse toResponse(Event event) {
        return toResponse(event, null);
    }

    public EventResponse toResponse(Event event, Double distanceKm) {
        int totalTickets = event.getTotalTickets() != null ? event.getTotalTickets() : 0;
        int booked = bookingRepository.sumBookedQuantityByEventId(event.getId()).intValue();
        List<TicketTierResponse> tiers = ticketTierRepository.findByEventIdOrderByIdAsc(event.getId()).stream()
                .map(this::toTierResponse)
                .toList();
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getTicketPrice(),
                Math.max(totalTickets - booked, 0),
                event.getImageUrl(),
                event.getLatitude(),
                event.getLongitude(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                distanceKm,
                tiers
        );
    }

    public TicketTierResponse toTierResponse(TicketTier tier) {
        int total = tier.getTotalQuantity() != null ? tier.getTotalQuantity() : 0;
        int sold = tier.getSoldQuantity() != null ? tier.getSoldQuantity() : 0;
        return new TicketTierResponse(
                tier.getId(),
                tier.getEvent().getId(),
                tier.getName(),
                tier.getPrice(),
                total,
                sold,
                Math.max(total - sold, 0),
                tier.getDescription(),
                tier.getCreatedAt()
        );
    }

    public EventBookingResponse toEventBookingResponse(Booking booking) {
        return new EventBookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getUser().getFullName(),
                booking.getQuantity(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getBookingDate()
        );
    }
}
