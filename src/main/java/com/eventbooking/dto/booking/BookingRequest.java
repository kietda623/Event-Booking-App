package com.eventbooking.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {
    @NotNull
    private Long eventId;

    private Integer quantity;
}
