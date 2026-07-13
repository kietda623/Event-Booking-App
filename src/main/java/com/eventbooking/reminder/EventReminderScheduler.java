package com.eventbooking.reminder;

import com.eventbooking.entity.Ticket;
import com.eventbooking.notification.NotificationService;
import com.eventbooking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(EventReminderScheduler.class);

    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    public void runDailyReminderJob() {
        sendDailyReminders();
    }

    public int sendDailyReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Ticket> tickets = ticketRepository.findReminderTickets(now, now.plusHours(24));
        if (tickets.isEmpty()) {
            log.info("event_reminders_sent count=0");
            return 0;
        }
        tickets.forEach(notificationService::sendReminderEmail);
        log.info("event_reminders_sent count={}", tickets.size());
        return tickets.size();
    }
}
