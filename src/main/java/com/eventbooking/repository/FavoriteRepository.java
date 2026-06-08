package com.eventbooking.repository;

import com.eventbooking.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserEmailOrderByEventEventDateAsc(String email);
    Page<Favorite> findByUserEmailOrderByEventEventDateAsc(String email, Pageable pageable);
    Optional<Favorite> findByUserEmailAndEventId(String email, Long eventId);
}
