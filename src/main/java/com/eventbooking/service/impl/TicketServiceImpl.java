package com.eventbooking.service.impl;

import com.eventbooking.dto.ticket.TicketCheckInRequest;
import com.eventbooking.dto.ticket.TicketCheckInResponse;
import com.eventbooking.entity.Ticket;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.TicketRepository;
import com.eventbooking.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public TicketCheckInResponse checkIn(TicketCheckInRequest request) {
        Ticket ticket = ticketRepository.findByTicketCode(request.getTicketCode())
                .orElseThrow(() -> new ResourceNotFoundException("TICKET_NOT_FOUND", "Ticket not found"));
        if ("CANCELLED".equals(ticket.getStatus()) || "CANCELLED".equals(ticket.getBooking().getStatus())) {
            throw new BusinessException("TICKET_CANCELLED", "Ticket is cancelled", HttpStatus.CONFLICT);
        }
        if (Boolean.TRUE.equals(ticket.getCheckedIn())) {
            throw new BusinessException("TICKET_ALREADY_CHECKED_IN", "Ticket has already been checked in", HttpStatus.CONFLICT);
        }

        ticket.setCheckedIn(true);
        ticket.setCheckedInAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        return toResponse(saved);
    }

    private TicketCheckInResponse toResponse(Ticket ticket) {
        var booking = ticket.getBooking();
        return new TicketCheckInResponse(
                ticket.getId(),
                ticket.getTicketCode(),
                ticket.getCheckedIn(),
                ticket.getCheckedInAt(),
                booking.getUser().getFullName(),
                booking.getEvent().getTitle(),
                ticket.getSeatNumber()
        );
    }
}
