package com.eventbooking.service.impl;

import com.eventbooking.dto.favorite.FavoriteResponse;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.Favorite;
import com.eventbooking.entity.User;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.FavoriteRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Override
    public FavoriteResponse toggle(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        String username = currentUsername();

        return favoriteRepository.findByUserUsernameAndEventId(username, eventId)
                .map(favorite -> {
                    favoriteRepository.delete(favorite);
                    return toResponse(event, false);
                })
                .orElseGet(() -> {
                    Favorite favorite = new Favorite();
                    favorite.setUser(currentUser());
                    favorite.setEvent(event);
                    favoriteRepository.save(favorite);
                    return toResponse(event, true);
                });
    }

    @Override
    public List<FavoriteResponse> myFavorites() {
        return favoriteRepository.findByUserUsernameOrderByEventEventDateAsc(currentUsername()).stream()
                .map(favorite -> toResponse(favorite.getEvent(), true))
                .toList();
    }

    private User currentUser() {
        return userRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private FavoriteResponse toResponse(Event event, boolean favorited) {
        return new FavoriteResponse(
                event.getId(),
                event.getTitle(),
                event.getEventDate(),
                event.getLocation(),
                event.getTicketPrice(),
                favorited
        );
    }
}
