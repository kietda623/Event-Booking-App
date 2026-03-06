package com.eventbooking.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventRequest {
    @NotBlank
    private String title;

    private String description;

    @NotNull
    private LocalDateTime eventDate;

    private String location;

    private Double price;

    private Integer totalTickets;
}
