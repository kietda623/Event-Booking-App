package com.eventbooking.repository;

import com.eventbooking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByBookingIdAndStatus(Long bookingId, String status);
    Optional<Payment> findByPaymentIntentId(String paymentIntentId);
    Optional<Payment> findFirstByBookingIdAndMethodOrderByIdDesc(Long bookingId, String method);
}
