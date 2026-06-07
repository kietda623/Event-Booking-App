package com.eventbooking.repository;

import com.eventbooking.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    Optional<Reminder> findByUserUsername(String username);
}
