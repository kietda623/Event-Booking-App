package com.eventbooking.service;

import com.eventbooking.dto.ticket.TicketCheckInRequest;
import com.eventbooking.dto.ticket.TicketCheckInResponse;

public interface TicketService {
    TicketCheckInResponse checkIn(TicketCheckInRequest request);
}
