package com.eventbooking.repository;

import com.eventbooking.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findFirstByBookingIdOrderByIdDesc(Long bookingId);
}
