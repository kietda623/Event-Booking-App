package com.eventbooking.repository;

import com.eventbooking.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findByBookingUserEmailOrderByIdDesc(String email, Pageable pageable);
    Optional<Ticket> findFirstByBookingIdOrderByIdDesc(Long bookingId);
    Optional<Ticket> findByTicketCode(String ticketCode);
    List<Ticket> findByBookingId(Long bookingId);
}
