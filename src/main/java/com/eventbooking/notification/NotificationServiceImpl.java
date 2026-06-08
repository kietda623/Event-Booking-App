package com.eventbooking.notification;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Ticket;
import com.eventbooking.entity.User;
import com.eventbooking.push.PushNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final MailSender mailSender;
    private final TemplateEngine templateEngine;
    private final PushNotificationService pushNotificationService;

    @Override
    @Async
    public void sendWelcomeEmail(User user) {
        Context context = new Context();
        context.setVariable("fullName", user.getFullName());
        String html = templateEngine.process("email/welcome", context);
        mailSender.send(new EmailMessage(user.getEmail(), "Welcome to Event Booking", html));
    }

    @Override
    @Async
    public void sendBookingPaidEmail(Booking booking, Ticket ticket) {
        Context context = bookingContext(booking);
        context.setVariable("ticketCode", ticket.getTicketCode());
        String html = templateEngine.process("email/booking-paid", context);
        mailSender.send(new EmailMessage(booking.getUser().getEmail(), "Booking confirmed: " + booking.getEvent().getTitle(), html));
        pushNotificationService.sendToUser(booking.getUser(), "Booking confirmed", booking.getEvent().getTitle());
    }

    @Override
    @Async
    public void sendBookingCancelledEmail(Booking booking) {
        Context context = bookingContext(booking);
        context.setVariable("bookingId", booking.getId());
        context.setVariable("amount", booking.getTotalPrice());
        String html = templateEngine.process("email/booking-cancelled", context);
        mailSender.send(new EmailMessage(booking.getUser().getEmail(), "Refund notice for booking " + booking.getId(), html));
        pushNotificationService.sendToUser(booking.getUser(), "Booking cancelled", "Refund pending for booking " + booking.getId());
    }

    @Override
    @Async
    public void sendReminderEmail(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Context context = bookingContext(booking);
        context.setVariable("ticketCode", ticket.getTicketCode());
        String html = templateEngine.process("email/event-reminder", context);
        mailSender.send(new EmailMessage(ticket.getUser().getEmail(), "Reminder: " + booking.getEvent().getTitle(), html));
        pushNotificationService.sendToUser(ticket.getUser(), "Event reminder", booking.getEvent().getTitle());
    }

    private Context bookingContext(Booking booking) {
        Context context = new Context();
        context.setVariable("eventName", booking.getEvent().getTitle());
        context.setVariable("eventDate", booking.getEvent().getEventDate());
        context.setVariable("location", booking.getEvent().getLocation());
        context.setVariable("quantity", booking.getQuantity());
        context.setVariable("bookingId", booking.getId());
        return context;
    }
}
