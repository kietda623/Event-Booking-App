package com.eventbooking.controller;

import com.eventbooking.dto.common.ApiResponse;
import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.dto.booking.EventBookingResponse;
import com.eventbooking.dto.event.EventRequest;
import com.eventbooking.dto.event.EventResponse;
import com.eventbooking.dto.favorite.FavoriteResponse;
import com.eventbooking.service.EventService;
import com.eventbooking.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events")
public class EventController {

    private final EventService eventService;
    private final FavoriteService favoriteService;

    @GetMapping
    @Operation(summary = "List events with optional type filters")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Events retrieved successfully")
    public ApiResponse<PageResponse<EventResponse>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean upcoming,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ){
        return ApiResponse.success("Events retrieved successfully",
                eventService.getAll(type, latitude, longitude, search, upcoming, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event detail")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event retrieved successfully")
    public ApiResponse<EventResponse> getById(@PathVariable Long id){
        return ApiResponse.success("Event retrieved successfully", eventService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an event")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Event created successfully")
    public ApiResponse<EventResponse> create(@Valid @RequestBody EventRequest request) {
        return ApiResponse.success("Event created successfully", eventService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an event")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event updated successfully")
    public ApiResponse<EventResponse> update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return ApiResponse.success("Event updated successfully", eventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an event")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event deleted successfully")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ApiResponse.success("Event deleted successfully", null);
    }

    @PostMapping("/{id}/favorite")
    @Operation(summary = "Favorite an event")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event favorited successfully")
    public ApiResponse<FavoriteResponse> addFavorite(@PathVariable Long id) {
        return ApiResponse.success("Event favorited successfully", favoriteService.add(id));
    }

    @DeleteMapping("/{id}/favorite")
    @Operation(summary = "Remove event favorite")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event favorite removed successfully")
    public ApiResponse<FavoriteResponse> removeFavorite(@PathVariable Long id) {
        return ApiResponse.success("Event favorite removed successfully", favoriteService.remove(id));
    }

    @GetMapping("/{id}/bookings")
    @Operation(summary = "List bookings for an event")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Event bookings retrieved successfully")
    public ApiResponse<PageResponse<EventBookingResponse>> getBookingsByEvent(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success("Event bookings retrieved successfully",
                eventService.getBookingsByEvent(id, page, size));
    }
}
