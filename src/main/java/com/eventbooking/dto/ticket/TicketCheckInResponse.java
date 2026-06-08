package com.eventbooking.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TicketCheckInResponse {
    private Long ticketId;
    private String ticketCode;
    private Boolean checkedIn;
    private LocalDateTime checkedInAt;
    private String attendeeName;
    private String eventTitle;
    private String seatNumber;
}
