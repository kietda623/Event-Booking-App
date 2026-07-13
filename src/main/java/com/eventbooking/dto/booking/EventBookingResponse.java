package com.eventbooking.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventBookingResponse {
    private Long bookingId;
    private Long userId;
    private String userName;
    private Integer quantity;
    private Double totalPrice;
    private String status;
    private LocalDateTime createdAt;
}
