package com.eventbooking.dto.booking;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    private Long bookingId;

    private Long eventId;

    private String eventTitle;

    private Integer quantity;

    private Double totalPrice;

    private LocalDateTime bookingTime;

    private String status;

    private String refundStatus;

    private Long tierId;

    private String tierName;

    private List<String> seatNumbers;

    public BookingResponse(Long bookingId, Long eventId, String eventTitle, Integer quantity, Double totalPrice,
                           LocalDateTime bookingTime, String status, String refundStatus) {
        this(bookingId, eventId, eventTitle, quantity, totalPrice, bookingTime, status, refundStatus,
                null, null, List.of());
    }

    public BookingResponse(Long bookingId, Long eventId, String eventTitle, Integer quantity, Double totalPrice,
                           LocalDateTime bookingTime, String status, String refundStatus,
                           Long tierId, String tierName, List<String> seatNumbers) {
        this.bookingId = bookingId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.bookingTime = bookingTime;
        this.status = status;
        this.refundStatus = refundStatus;
        this.tierId = tierId;
        this.tierName = tierName;
        this.seatNumbers = seatNumbers == null ? List.of() : seatNumbers;
    }
}
