package com.eventbooking.controller;

import com.eventbooking.dto.ticket.TicketResponse;
import com.eventbooking.entity.Ticket;
import com.eventbooking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketRepository ticketRepository;

    @GetMapping
    public List<TicketResponse> myTickets() {
        return ticketRepository.findByBookingUserUsernameOrderByIdDesc(currentUsername()).stream()
                .map(this::toResponse)
                .toList();
    }

    private TicketResponse toResponse(Ticket ticket) {
        var booking = ticket.getBooking();
        var event = booking.getEvent();
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketCode(),
                event.getId(),
                event.getTitle(),
                event.getEventDate(),
                event.getLocation(),
                booking.getQuantity(),
                booking.getStatus()
        );
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
