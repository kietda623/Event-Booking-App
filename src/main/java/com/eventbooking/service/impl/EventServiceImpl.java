package com.eventbooking.service.impl;

import com.eventbooking.dto.booking.EventBookingResponse;
import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.dto.event.EventRequest;
import com.eventbooking.dto.event.EventResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "title", "eventDate", "location", "ticketPrice");

    @Override
    public PageResponse<EventResponse> getAll(String type, Double latitude, Double longitude, String search,
                                              boolean upcoming, int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "eventDate";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedType = type == null || type.isBlank() ? null : type.trim().toLowerCase();
        if (normalizedType == null && upcoming) {
            normalizedType = "upcoming";
        }

        Page<Event> events = switch (normalizedType == null ? "default" : normalizedType) {
            case "popular" -> eventRepository.findPopular(PageRequest.of(safePage, safeSize));
            case "upcoming" -> eventRepository.searchEvents(
                    normalizedSearch,
                    true,
                    LocalDateTime.now(),
                    PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "eventDate"))
            );
            case "nearby" -> {
                if (latitude == null || longitude == null) {
                    throw new BusinessException("MISSING_LOCATION_PARAMS",
                            "latitude and longitude are required for nearby events",
                            HttpStatus.BAD_REQUEST);
                }
                yield eventRepository.findNearby(latitude, longitude, PageRequest.of(safePage, safeSize));
            }
            default -> eventRepository.searchAllEvents(
                    normalizedSearch,
                    PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy))
            );
        };
        return toPageResponse(events.map(this::toResponse));
    }

    @Override
    public EventResponse getById(Long id) {
        return toResponse(findEvent(id));
    }

    @Override
    public EventResponse create(EventRequest request) {
        Event event = new Event();
        apply(event, request);
        return toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse update(Long id, EventRequest request) {
        Event event = findEvent(id);
        apply(event, request);
        return toResponse(eventRepository.save(event));
    }

    @Override
    public void delete(Long id) {
        Event event = findEvent(id);
        eventRepository.delete(event);
    }

    @Override
    public PageResponse<EventBookingResponse> getBookingsByEvent(Long eventId, int page, int size) {
        Event event = findEvent(eventId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Booking> bookings = bookingRepository.findByEventIdOrderByBookingDateDesc(
                event.getId(),
                PageRequest.of(safePage, safeSize)
        );
        return toPageResponse(bookings.map(this::toEventBookingResponse));
    }

    private Event findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EVENT_NOT_FOUND", "Event not found"));
    }

    private void apply(Event event, EventRequest request) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());
        event.setTicketPrice(request.getPrice());
        event.setTotalTickets(request.getTotalTickets());
        event.setImageUrl(request.getImageUrl());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());
    }

    private EventResponse toResponse(Event event) {
        int totalTickets = event.getTotalTickets() != null ? event.getTotalTickets() : 0;
        int booked = bookingRepository.sumBookedQuantityByEventId(event.getId()).intValue();
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
                event.getUpdatedAt()
        );
    }

    private EventBookingResponse toEventBookingResponse(Booking booking) {
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
