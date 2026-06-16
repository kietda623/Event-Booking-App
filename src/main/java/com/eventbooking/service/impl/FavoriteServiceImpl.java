package com.eventbooking.service.impl;

import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.dto.event.EventResponse;
import com.eventbooking.dto.favorite.FavoriteResponse;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.Favorite;
import com.eventbooking.entity.User;
import com.eventbooking.exception.BusinessException;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.FavoriteRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.service.FavoriteService;
import com.eventbooking.util.PageResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    public FavoriteResponse toggle(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("EVENT_NOT_FOUND", "Event not found"));
        String email = currentEmail();

        return favoriteRepository.findByUserEmailAndEventId(email, eventId)
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
    @Transactional
    public FavoriteResponse add(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("EVENT_NOT_FOUND", "Event not found"));
        if (favoriteRepository.findByUserEmailAndEventId(currentEmail(), eventId).isPresent()) {
            throw new BusinessException("ALREADY_FAVORITED", "Event is already favorited", HttpStatus.CONFLICT);
        }
        Favorite favorite = new Favorite();
        favorite.setUser(currentUser());
        favorite.setEvent(event);
        favoriteRepository.save(favorite);
        return toResponse(event, true);
    }

    @Override
    @Transactional
    public FavoriteResponse remove(Long eventId) {
        Favorite favorite = favoriteRepository.findByUserEmailAndEventId(currentEmail(), eventId)
                .orElseThrow(() -> new ResourceNotFoundException("FAVORITE_NOT_FOUND", "Favorite not found"));
        Event event = favorite.getEvent();
        favoriteRepository.delete(favorite);
        return toResponse(event, false);
    }

    @Override
    public List<FavoriteResponse> myFavorites() {
        return favoriteRepository.findByUserEmailOrderByEventEventDateAsc(currentEmail()).stream()
                .map(favorite -> toResponse(favorite.getEvent(), true))
                .toList();
    }

    @Override
    public PageResponse<EventResponse> myFavoriteEvents(int page, int size) {
        Page<EventResponse> favorites = favoriteRepository.findByUserEmailOrderByEventEventDateAsc(
                currentEmail(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))
        ).map(favorite -> toEventResponse(favorite.getEvent()));
        return PageResponseMapper.from(favorites);
    }

    private User currentUser() {
        return userRepository.findByEmail(currentEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String currentEmail() {
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

    private EventResponse toEventResponse(Event event) {
        int totalTickets = event.getTotalTickets() != null ? event.getTotalTickets() : 0;
        int booked = bookingRepository.sumBookedQuantityByEventId(event.getId()).intValue();
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getTicketPrice(),
                Math.max(totalTickets - booked, 0),
                event.getImageUrl(),
                event.getLatitude(),
                event.getLongitude(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

}
