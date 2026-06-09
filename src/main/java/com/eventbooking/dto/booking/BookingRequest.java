package com.eventbooking.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BookingRequest {
    @NotNull
    private Long eventId;

    @NotNull
    private Long tierId;

    private Integer quantity;

    private List<String> seatNumbers;
}
