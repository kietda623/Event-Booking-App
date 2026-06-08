package com.eventbooking.repository;

import com.eventbooking.entity.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    @Query("""
            select e from Event e
            where (:upcoming = false or e.eventDate > :now)
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

    @Query("""
            select e from Event e
            where (:search is null
                or lower(e.title) like lower(concat('%', :search, '%'))
                or lower(e.location) like lower(concat('%', :search, '%')))
            """)
    Page<Event> searchAllEvents(@Param("search") String search, Pageable pageable);

    @Query(value = """
            select e from Event e
            left join Booking b on b.event = e and b.status = 'PAID'
            group by e
            order by coalesce(sum(b.quantity), 0) desc, e.eventDate asc
            """,
            countQuery = "select count(e) from Event e")
    Page<Event> findPopular(Pageable pageable);

    @Query(
            value = """
                    select e.* from events e
                    where e.latitude is not null
                      and e.longitude is not null
                      and (6371 * 2 * asin(sqrt(
                          power(sin(radians(e.latitude - :latitude) / 2), 2) +
                          cos(radians(:latitude)) * cos(radians(e.latitude)) *
                          power(sin(radians(e.longitude - :longitude) / 2), 2)
                      ))) <= 50
                    order by (6371 * 2 * asin(sqrt(
                          power(sin(radians(e.latitude - :latitude) / 2), 2) +
                          cos(radians(:latitude)) * cos(radians(e.latitude)) *
                          power(sin(radians(e.longitude - :longitude) / 2), 2)
                    ))) asc
                    """,
            countQuery = """
                    select count(*) from events e
                    where e.latitude is not null
                      and e.longitude is not null
                      and (6371 * 2 * asin(sqrt(
                          power(sin(radians(e.latitude - :latitude) / 2), 2) +
                          cos(radians(:latitude)) * cos(radians(e.latitude)) *
                          power(sin(radians(e.longitude - :longitude) / 2), 2)
                      ))) <= 50
                    """,
            nativeQuery = true
    )
    Page<Event> findNearby(@Param("latitude") Double latitude, @Param("longitude") Double longitude, Pageable pageable);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForBooking(@Param("id") Long id);
}
