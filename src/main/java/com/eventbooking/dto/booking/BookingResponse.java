package com.eventbooking.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BookingResponse {
    private Long bookingId;

    private Long eventId;

    private String eventTitle;

    private Integer quantity;

    private Double totalPrice;

    private LocalDateTime bookingTime;
}
