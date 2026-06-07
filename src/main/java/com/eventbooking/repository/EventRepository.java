package com.eventbooking.repository;

import com.eventbooking.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("""
            select e from Event e
            where (:upcoming = false or e.eventDate >= :now)
              and (:search is null
                or lower(e.title) like lower(concat('%', :search, '%'))
                or lower(e.location) like lower(concat('%', :search, '%')))
            """)
    Page<Event> searchEvents(
            @Param("search") String search,
            @Param("upcoming") boolean upcoming,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
