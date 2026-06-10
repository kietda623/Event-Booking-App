package com.eventbooking.repository;

import com.eventbooking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query(
            value = "select b from Booking b where b.user.email = :email order by b.bookingDate desc",
            countQuery = "select count(b) from Booking b where b.user.email = :email"
    )
    Page<Booking> findByUserEmailOrderByBookingDateDesc(@Param("email") String email, Pageable pageable);
    Optional<Booking> findByIdAndUserEmail(Long id, String email);

    @Query(
            value = "select b from Booking b where b.event.id = :eventId order by b.bookingDate desc",
            countQuery = "select count(b) from Booking b where b.event.id = :eventId"
    )
    Page<Booking> findByEventIdOrderByBookingDateDesc(@Param("eventId") Long eventId, Pageable pageable);

    @Query("select coalesce(sum(b.quantity), 0) from Booking b where b.event.id = :eventId and b.status <> 'CANCELLED'")
    Long sumBookedQuantityByEventId(@Param("eventId") Long eventId);

    long countByStatus(String status);

    long countByEventId(Long eventId);

    @Query("select coalesce(sum(b.totalPrice), 0) from Booking b where b.status = 'PAID'")
    Double sumPaidRevenue();

    @Query("""
            select b.event.id, b.event.title, coalesce(sum(b.quantity), 0)
            from Booking b
            where b.status = 'PAID'
            group by b.event.id, b.event.title
            order by coalesce(sum(b.quantity), 0) desc
            """)
    List<Object[]> findTopEventsByPaidQuantity(Pageable pageable);
}
