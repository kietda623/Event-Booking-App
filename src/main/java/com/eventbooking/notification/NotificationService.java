package com.eventbooking.notification;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Ticket;
import com.eventbooking.entity.User;

public interface NotificationService {
    void sendWelcomeEmail(User user);
    void sendBookingPaidEmail(Booking booking, Ticket ticket);
    void sendBookingCancelledEmail(Booking booking);
    void sendReminderEmail(Ticket ticket);
}
