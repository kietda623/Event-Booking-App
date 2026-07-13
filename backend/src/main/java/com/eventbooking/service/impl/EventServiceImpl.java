package com.eventbooking.service.impl;

import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.dto.event.EventRequest;
import com.eventbooking.dto.event.EventResponse;
import com.eventbooking.entity.Event;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public PageResponse<EventResponse> getAll(String search, boolean upcoming, int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "eventDate";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();

        var events = eventRepository.searchEvents(
                normalizedSearch,
                upcoming,
                LocalDateTime.now(),
                PageRequest.of(safePage, safeSize, Sort.by(direction, safeSortBy))
        );
        return new PageResponse<>(
                events.getContent().stream().map(this::toResponse).toList(),
                events.getTotalElements(),
                events.getTotalPages(),
                events.getNumber(),
                events.getSize()
        );
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

    private Event findEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private void apply(Event event, EventRequest request) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(request.getEventDate());
        event.setLocation(request.getLocation());
        event.setTicketPrice(request.getPrice());
        event.setTotalTickets(request.getTotalTickets());
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
                Math.max(totalTickets - booked, 0)
        );
    }
}
