package com.eventbooking.repository;

import com.eventbooking.entity.TicketTier;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {
    List<TicketTier> findByEventIdOrderByIdAsc(Long eventId);

    Optional<TicketTier> findByIdAndEventId(Long id, Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TicketTier t where t.id = :id")
    Optional<TicketTier> findByIdForBooking(@Param("id") Long id);
}
