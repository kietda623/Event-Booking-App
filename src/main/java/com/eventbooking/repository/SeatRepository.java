package com.eventbooking.repository;

import com.eventbooking.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByEventIdOrderByRowAscColAscIdAsc(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.event.id = :eventId and s.seatNumber in :seatNumbers")
    List<Seat> findByEventIdAndSeatNumbersForUpdate(
            @Param("eventId") Long eventId,
            @Param("seatNumbers") List<String> seatNumbers
    );

    List<Seat> findByEventIdAndSeatNumberIn(Long eventId, List<String> seatNumbers);

    @Modifying
    @Query("""
            update Seat s
            set s.status = 'AVAILABLE', s.heldUntil = null, s.heldByUserId = null
            where s.event.id = :eventId
              and s.heldByUserId = :userId
              and s.status = 'HELD'
            """)
    int releaseHeldSeats(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Modifying
    @Query("""
            update Seat s
            set s.status = 'AVAILABLE', s.heldUntil = null, s.heldByUserId = null
            where s.status = 'HELD'
              and s.heldUntil < :now
            """)
    int releaseExpiredHolds(@Param("now") LocalDateTime now);
}
