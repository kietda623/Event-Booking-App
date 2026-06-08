package com.eventbooking.service;

import com.eventbooking.dto.booking.BookingRequest;
import com.eventbooking.dto.booking.BookingResponse;
import com.eventbooking.dto.common.PageResponse;

public interface BookingService {
    BookingResponse book(BookingRequest request);
    PageResponse<BookingResponse> myBookings(int page, int size);
    BookingResponse cancel(Long id);
    BookingResponse cancelPending(Long id);
}
