package com.eventbooking.service;

import com.eventbooking.dto.favorite.FavoriteResponse;

import java.util.List;

public interface FavoriteService {
    FavoriteResponse toggle(Long eventId);
    List<FavoriteResponse> myFavorites();
}
