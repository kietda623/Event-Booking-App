package com.eventbooking.repository;

import com.eventbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserUsernameOrderByBookingDateDesc(String username);
    Optional<Booking> findByIdAndUserUsername(Long id, String username);

    @Query("select coalesce(sum(b.quantity), 0) from Booking b where b.event.id = :eventId and b.status <> 'CANCELLED'")
    Long sumBookedQuantityByEventId(@Param("eventId") Long eventId);
}
