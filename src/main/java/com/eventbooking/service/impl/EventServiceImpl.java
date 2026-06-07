package com.eventbooking.service.impl;

import com.eventbooking.dto.event.EventResponse;
import com.eventbooking.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    @Override
    public List<EventResponse> getAll() {
        return List.of(new EventResponse(1L, "Sample Event", "Placeholder event.", LocalDateTime.now(), "Online", 0.0, 0));
    }

    @Override
    public EventResponse getById(Long id) {
        return new EventResponse(id, "Sample Event", "Placeholder event.", LocalDateTime.now(), "Online", 0.0, 0);
    }
}
