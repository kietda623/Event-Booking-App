package com.eventbooking.repository;

import com.eventbooking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByBookingIdAndStatus(Long bookingId, String status);
}
