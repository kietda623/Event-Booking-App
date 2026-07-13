package com.eventbooking.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventResponse {
    private long id;

    private String title;

    private String description;

    private LocalDateTime eventDate;

    private String location;

    private Double price;

    private Integer availableTickets;
}
