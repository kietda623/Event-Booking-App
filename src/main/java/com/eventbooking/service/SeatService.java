package com.eventbooking.service;

import com.eventbooking.dto.seat.SeatHoldRequest;
import com.eventbooking.dto.seat.SeatResponse;
import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.TicketTier;
import com.eventbooking.entity.User;

import java.util.List;

public interface SeatService {
    List<SeatResponse> list(Long eventId);
    List<SeatResponse> hold(Long eventId, SeatHoldRequest request);
    void releaseHeld(Long eventId);
    void validateHeldSeats(Event event, TicketTier tier, User user, List<String> seatNumbers);
    void bookSeats(Booking booking);
    void releaseBookingSeats(Booking booking);
}
