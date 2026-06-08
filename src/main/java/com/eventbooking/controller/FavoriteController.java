package com.eventbooking.controller;

import com.eventbooking.dto.common.ApiResponse;
import com.eventbooking.dto.favorite.FavoriteResponse;
import com.eventbooking.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping("/{eventId}")
    @Operation(summary = "Toggle event favorite")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Favorite updated successfully")
    public ApiResponse<FavoriteResponse> toggle(@PathVariable Long eventId) {
        return ApiResponse.success("Favorite updated successfully", favoriteService.toggle(eventId));
    }

    @GetMapping
    @Operation(summary = "List current user's favorites")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Favorites retrieved successfully")
    public ApiResponse<List<FavoriteResponse>> myFavorites() {
        return ApiResponse.success("Favorites retrieved successfully", favoriteService.myFavorites());
    }
}
