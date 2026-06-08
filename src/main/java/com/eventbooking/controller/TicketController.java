package com.eventbooking.controller;

import com.eventbooking.dto.common.ApiResponse;
import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.dto.ticket.TicketCheckInRequest;
import com.eventbooking.dto.ticket.TicketCheckInResponse;
import com.eventbooking.dto.ticket.TicketResponse;
import com.eventbooking.entity.Ticket;
import com.eventbooking.repository.TicketRepository;
import com.eventbooking.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets")
public class TicketController {
    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    @GetMapping
    @Operation(summary = "List current user's tickets")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tickets retrieved successfully")
    public ApiResponse<PageResponse<TicketResponse>> myTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<TicketResponse> tickets = ticketRepository.findByBookingUserEmailOrderByIdDesc(
                currentEmail(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        ).map(this::toResponse);
        return ApiResponse.success("Tickets retrieved successfully", toPageResponse(tickets));
    }

    @PostMapping("/checkin")
    @Operation(summary = "Check in a ticket")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ticket checked in successfully")
    public ApiResponse<TicketCheckInResponse> checkIn(@Valid @RequestBody TicketCheckInRequest request) {
        return ApiResponse.success("Ticket checked in successfully", ticketService.checkIn(request));
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
                booking.getStatus(),
                ticket.getTicketType(),
                ticket.getSeatNumber(),
                ticket.getCheckedIn(),
                ticket.getCheckedInAt()
        );
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
