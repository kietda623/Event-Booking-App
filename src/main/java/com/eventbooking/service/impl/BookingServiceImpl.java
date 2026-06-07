package com.eventbooking.service.impl;

import com.eventbooking.dto.booking.BookingRequest;
import com.eventbooking.dto.booking.BookingResponse;
import com.eventbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    @Override
    public BookingResponse book(BookingRequest request) {
        return new BookingResponse(
                null,
                request.getEventId(),
                "Stub Event",
                request.getQuantity() != null ? request.getQuantity() : 1,
                0.0,
                LocalDateTime.now()
        );
    }

    @Override
    public List<BookingResponse> myBookings() {
        return List.of();
    }
}
