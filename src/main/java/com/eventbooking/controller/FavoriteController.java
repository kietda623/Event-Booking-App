package com.eventbooking.controller;

import com.eventbooking.dto.favorite.FavoriteResponse;
import com.eventbooking.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping("/{eventId}")
    public FavoriteResponse toggle(@PathVariable Long eventId) {
        return favoriteService.toggle(eventId);
    }

    @GetMapping
    public List<FavoriteResponse> myFavorites() {
        return favoriteService.myFavorites();
    }
}
