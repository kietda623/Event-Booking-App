package com.eventbooking.service;

import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.dto.event.EventResponse;
import com.eventbooking.dto.event.EventRequest;

public interface EventService {
    PageResponse<EventResponse> getAll(String search, boolean upcoming, int page, int size, String sortBy, String sortDir);
    EventResponse getById(Long id);
    EventResponse create(EventRequest request);
    EventResponse update(Long id, EventRequest request);
    void delete(Long id);
}
