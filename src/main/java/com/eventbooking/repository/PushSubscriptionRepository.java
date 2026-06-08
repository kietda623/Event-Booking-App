package com.eventbooking.repository;

import com.eventbooking.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    List<PushSubscription> findByUserEmail(String email);
    Optional<PushSubscription> findByEndpoint(String endpoint);
    void deleteByUserEmailAndEndpoint(String email, String endpoint);
}
