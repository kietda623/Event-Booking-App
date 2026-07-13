package com.eventbooking.dto.ticket;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TicketResponse {
    private Long ticketId;
    private String ticketCode;
    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String location;
    private Integer quantity;
    private String status;
    private String ticketType;
    private String seatNumber;
    private Boolean checkedIn;
    private LocalDateTime checkedInAt;
}
