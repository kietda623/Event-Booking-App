package com.eventbooking.service;

import com.eventbooking.dto.favorite.FavoriteResponse;
import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.dto.event.EventResponse;

import java.util.List;

public interface FavoriteService {
    FavoriteResponse toggle(Long eventId);
    FavoriteResponse add(Long eventId);
    FavoriteResponse remove(Long eventId);
    List<FavoriteResponse> myFavorites();
    PageResponse<EventResponse> myFavoriteEvents(int page, int size);
}
