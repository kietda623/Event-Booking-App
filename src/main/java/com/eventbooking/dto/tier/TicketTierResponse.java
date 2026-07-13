package com.eventbooking.dto.tier;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TicketTierResponse {
    private Long id;
    private Long eventId;
    private String name;
    private Double price;
    private Integer totalQuantity;
    private Integer soldQuantity;
    private Integer availableQuantity;
    private String description;
    private LocalDateTime createdAt;
}
