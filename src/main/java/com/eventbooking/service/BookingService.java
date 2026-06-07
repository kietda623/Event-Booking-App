package com.eventbooking.service;

import com.eventbooking.dto.booking.BookingRequest;
import com.eventbooking.dto.booking.BookingResponse;

import java.util.List;

public interface BookingService {
    BookingResponse book(BookingRequest request);
    List<BookingResponse> myBookings();
    BookingResponse cancel(Long id);
}
