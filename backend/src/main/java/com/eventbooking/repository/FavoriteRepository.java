package com.eventbooking.repository;

import com.eventbooking.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserUsernameOrderByEventEventDateAsc(String username);
    Optional<Favorite> findByUserUsernameAndEventId(String username, Long eventId);
}
