package com.eventbooking.repository;

import com.eventbooking.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Query(
            value = "select t from Ticket t where t.user.email = :email order by t.id desc",
            countQuery = "select count(t) from Ticket t where t.user.email = :email"
    )
    Page<Ticket> findByUserEmailOrderByIdDesc(@Param("email") String email, Pageable pageable);

    Page<Ticket> findByBookingUserEmailOrderByIdDesc(String email, Pageable pageable);
    Optional<Ticket> findFirstByBookingIdOrderByIdDesc(Long bookingId);
    Optional<Ticket> findByTicketCode(String ticketCode);
    List<Ticket> findByBookingId(Long bookingId);
}
