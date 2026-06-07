package com.eventbooking.service;

import com.eventbooking.dto.event.EventResponse;

import java.util.List;

public interface EventService {
    List<EventResponse> getAll();
    EventResponse getById(Long id);
}
