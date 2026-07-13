package com.eventbooking.repository;

import com.eventbooking.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByBookingUserUsernameOrderByIdDesc(String username);
}
