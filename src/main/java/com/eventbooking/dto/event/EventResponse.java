package com.eventbooking.dto.event;

import com.eventbooking.dto.tier.TicketTierResponse;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventResponse {
    private long id;

    private String title;

    private String description;

    private LocalDateTime eventDate;

    private String location;

    private Double price;

    private Integer availableTickets;

    private String imageUrl;

    private Double latitude;

    private Double longitude;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Double distanceKm;

    private List<TicketTierResponse> tiers;

    public EventResponse(long id, String title, String description, LocalDateTime eventDate, String location,
                         Double price, Integer availableTickets, String imageUrl, Double latitude, Double longitude,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, title, description, eventDate, location, price, availableTickets, imageUrl, latitude, longitude,
                createdAt, updatedAt, null, List.of());
    }

    public EventResponse(long id, String title, String description, LocalDateTime eventDate, String location,
                         Double price, Integer availableTickets, String imageUrl, Double latitude, Double longitude,
                         LocalDateTime createdAt, LocalDateTime updatedAt, Double distanceKm,
                         List<TicketTierResponse> tiers) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.location = location;
        this.price = price;
        this.availableTickets = availableTickets;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.distanceKm = distanceKm;
        this.tiers = tiers == null ? List.of() : tiers;
    }
}
