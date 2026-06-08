package com.eventbooking.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketCheckInRequest {
    @NotBlank
    private String ticketCode;
}
