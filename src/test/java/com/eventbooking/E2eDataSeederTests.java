package com.eventbooking;

import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.TicketRepository;
import com.eventbooking.repository.TicketTierRepository;
import com.eventbooking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "app.e2e.seed=true",
        "app.e2e.admin-email=admin.e2e@example.com",
        "app.e2e.admin-password=Admin123!",
        "app.e2e.user-email=user.e2e@example.com",
        "app.e2e.user-password=User123!",
        "app.e2e.checkin-ticket-code=E2E-CHECKIN-0001"
})
@ActiveProfiles({"test", "e2e"})
class E2eDataSeederTests {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTierRepository ticketTierRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void seedsDeterministicUsersEventTierAndCheckinTicket() {
        var admin = userRepository.findByEmail("admin.e2e@example.com");
        var testUser = userRepository.findByEmail("user.e2e@example.com");
        var event = eventRepository.findAll().stream()
                .filter(item -> "E2E Seed Future Festival".equals(item.getTitle()))
                .findFirst();
        var ticket = ticketRepository.findByTicketCode("E2E-CHECKIN-0001");

        assertThat(admin).isPresent();
        assertThat(admin.get().getRoles()).extracting("name").contains("ADMIN", "USER");
        assertThat(testUser).isPresent();
        assertThat(event).isPresent();
        assertThat(ticketTierRepository.findByEventIdOrderByIdAsc(event.get().getId()))
                .anySatisfy(tier -> {
                    assertThat(tier.getName()).isEqualTo("GENERAL");
                    assertThat(tier.getTotalQuantity()).isEqualTo(200);
                });
        assertThat(ticket).isPresent();
        assertThat(ticket.get().getCheckedIn()).isFalse();
        assertThat(ticket.get().getBooking().getStatus()).isEqualTo("PAID");
    }
}
