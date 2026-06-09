package com.eventbooking.dto.seat;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private Long eventId;
    private Long tierId;
    private String seatNumber;
    private String row;
    private Integer col;
    private String status;
    private LocalDateTime heldUntil;
    private Long heldByUserId;
}
