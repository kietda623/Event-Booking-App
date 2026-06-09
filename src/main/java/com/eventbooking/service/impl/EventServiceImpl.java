package com.eventbooking.service.impl;

import com.eventbooking.dto.booking.EventBookingResponse;
import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.dto.event.EventRequest;
import com.eventbooking.dto.event.EventResponse;
import com.eventbooking.dto.tier.TicketTierResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.TicketTier;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.TicketTierRepository;
import com.eventbooking.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final TicketTierRepository ticketTierRepository;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "title", "eventDate", "location", "ticketPrice");

    @Override
    @Cacheable(
            value = "events",
            key = "{#type, #latitude, #longitude, #search, #upcoming, #radius, #page, #size, #sortBy, #sortDir}"
    )
    public PageResponse<EventResponse> getAll(String type, Double latitude, Double longitude, String search,
                                              boolean upcoming, Double radius, int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "eventDate";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedType = type == null || type.isBlank() ? null : type.trim().toLowerCase();
        if (normalizedType == null && upcoming) {
            normalizedType = "upcoming";
        }

        if ("nearby".equals(normalizedType)) {
            return getNearby(latitude, longitude, radius, safePage, safeSize);
        }

        Page<Event> events = switch (normalizedType == null ? "default" : normalizedType) {
            case "popular" -> eventRepository.findPopular(PageRequest.of(safePage, safeSize));
            case "upcoming" -> eventRepository.searchEvents(
                    normalizedSearch,
                    true,
                    LocalDateTime.now(),
                    PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "eventDate"))
            );
            default -> eventRepository.searchAllEvents(
                    normalizedSearch,
                    PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy))
            );
        };
        return toPageResponse(events.map(this::toResponse));
    }

    @Override
    public List<EventResponse> nearbyPreview(Double latitude, Double longitude) {
        return getNearby(latitude, longitude, 200.0, 0, 4).getContent();
    }

    @Override
    public EventResponse getById(Long id) {
        return toResponse(findEvent(id));
    }

    @Override
    @CacheEvict(value = "events", allEntries = true)
    public EventResponse create(EventRequest request) {
        Event event = new Event();
        apply(event, request);
        return toResponse(eventRepository.save(event));
    }

    @Override
    @CacheEvict(value = "events", allEntries = true)
    public EventResponse update(Long id, EventRequest request) {
        Event event = findEvent(id);
        apply(event, request);
        return toResponse(eventRepository.save(event));
    }

    @Override
    @CacheEvict(value = "events", allEntries = true)
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

    private PageResponse<EventResponse> getNearby(Double latitude, Double longitude, Double radius, int page, int size) {
        if (latitude == null || longitude == null) {
            throw new BusinessException("MISSING_LOCATION_PARAMS",
                    "latitude and longitude are required for nearby events",
                    HttpStatus.BAD_REQUEST);
        }
        double safeRadius = radius == null ? 50.0 : radius;
        if (safeRadius <= 0 || safeRadius > 200) {
            throw new BusinessException("INVALID_RADIUS",
                    "radius must be greater than 0 and less than or equal to 200km",
                    HttpStatus.BAD_REQUEST);
        }

        Page<EventRepository.NearbyEventDistance> nearby = eventRepository.findNearbyDistances(
                latitude,
                longitude,
                safeRadius,
                PageRequest.of(page, size)
        );
        List<Long> ids = nearby.getContent().stream()
                .map(EventRepository.NearbyEventDistance::getId)
                .toList();
        Map<Long, Double> distanceById = nearby.getContent().stream()
                .collect(Collectors.toMap(
                        EventRepository.NearbyEventDistance::getId,
                        EventRepository.NearbyEventDistance::getDistanceKm,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<Long, Event> eventsById = eventRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));
        List<EventResponse> content = ids.stream()
                .map(eventsById::get)
                .filter(event -> event != null)
                .map(event -> toResponse(event, distanceById.get(event.getId())))
                .toList();
        return toPageResponse(new PageImpl<>(content, nearby.getPageable(), nearby.getTotalElements()));
    }

    private EventResponse toResponse(Event event) {
        return toResponse(event, null);
    }

    private EventResponse toResponse(Event event, Double distanceKm) {
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

    private TicketTierResponse toTierResponse(TicketTier tier) {
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
