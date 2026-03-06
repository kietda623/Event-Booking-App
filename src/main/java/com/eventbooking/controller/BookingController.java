package com.eventbooking.controller;

import com.eventbooking.dto.booking.BookingRequest;
import com.eventbooking.dto.booking.BookingResponse;
import com.eventbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public BookingResponse book(@RequestBody BookingRequest request){
        return bookingService.book(request);
    }

    @GetMapping("/my")
    public List<BookingResponse> myBookings(){
        return bookingService.myBookings();
    }
}
