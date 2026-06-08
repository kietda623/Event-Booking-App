package com.eventbooking.controller;

import com.eventbooking.dto.booking.BookingRequest;
import com.eventbooking.dto.booking.BookingResponse;
import com.eventbooking.dto.common.ApiResponse;
import com.eventbooking.dto.common.PageResponse;
import com.eventbooking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a booking")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Booking created successfully")
    public ApiResponse<BookingResponse> book(@Valid @RequestBody BookingRequest request){
        return ApiResponse.success("Booking created successfully", bookingService.book(request));
    }

    @GetMapping("/my")
    @Operation(summary = "List current user's bookings")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Bookings retrieved successfully")
    public ApiResponse<PageResponse<BookingResponse>> myBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return ApiResponse.success("Bookings retrieved successfully", bookingService.myBookings(page, size));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending booking")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking cancelled successfully")
    public ApiResponse<BookingResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success("Booking cancelled successfully", bookingService.cancelPending(id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending or paid booking")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking cancelled successfully")
    public ApiResponse<BookingResponse> cancelPhase4(@PathVariable Long id) {
        return ApiResponse.success("Booking cancelled successfully", bookingService.cancel(id));
    }
}
