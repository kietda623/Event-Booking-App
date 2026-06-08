package com.eventbooking.repository;

import com.eventbooking.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserEmailOrderByEventEventDateAsc(String email);

    @Query(
            value = "select f from Favorite f where f.user.email = :email order by f.event.eventDate asc",
            countQuery = "select count(f) from Favorite f where f.user.email = :email"
    )
    Page<Favorite> findByUserEmailOrderByEventEventDateAsc(@Param("email") String email, Pageable pageable);

    Optional<Favorite> findByUserEmailAndEventId(String email, Long eventId);
}
