package com.eventbooking.config;

import com.eventbooking.entity.Booking;
import com.eventbooking.entity.Event;
import com.eventbooking.entity.Role;
import com.eventbooking.entity.Ticket;
import com.eventbooking.entity.TicketTier;
import com.eventbooking.entity.User;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.RoleRepository;
import com.eventbooking.repository.TicketRepository;
import com.eventbooking.repository.TicketTierRepository;
import com.eventbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@Profile("e2e")
@RequiredArgsConstructor
public class E2eDataSeeder implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.e2e.seed:false}")
    private boolean seedEnabled;

    @Value("${app.e2e.admin-email:admin.e2e@example.com}")
    private String adminEmail;

    @Value("${app.e2e.admin-password:Admin123!}")
    private String adminPassword;

    @Value("${app.e2e.user-email:user.e2e@example.com}")
    private String testUserEmail;

    @Value("${app.e2e.user-password:User123!}")
    private String testUserPassword;

    @Value("${app.e2e.checkin-ticket-code:E2E-CHECKIN-0001}")
    private String checkinTicketCode;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER")));
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN")));

        User admin = upsertUser(adminEmail, "E2E Admin", adminPassword, Set.of(userRole, adminRole));
        User testUser = upsertUser(testUserEmail, "E2E User", testUserPassword, Set.of(userRole));
        Event event = upsertSeedEvent();
        TicketTier tier = upsertSeedTier(event);
        upsertCheckinTicket(testUser, event, tier);
    }

    private User upsertUser(String emailValue, String fullName, String password, Set<Role> roles) {
        String email = emailValue.trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setUsername(email);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(new HashSet<>(roles));
        return userRepository.save(user);
    }

    private Event upsertSeedEvent() {
        Event event = eventRepository.findAll().stream()
                .filter(item -> "E2E Seed Future Festival".equals(item.getTitle()))
                .findFirst()
                .orElseGet(Event::new);
        event.setTitle("E2E Seed Future Festival");
        event.setDescription("Deterministic event used by Playwright E2E tests.");
        event.setLocation("E2E Saigon Hall");
        event.setEventDate(LocalDateTime.now().plusDays(30).withSecond(0).withNano(0));
        event.setTotalTickets(200);
        event.setTicketPrice(100000.0);
        event.setImageUrl("https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1400&q=80");
        event.setLatitude(10.776);
        event.setLongitude(106.700);
        return eventRepository.save(event);
    }

    private TicketTier upsertSeedTier(Event event) {
        TicketTier tier = ticketTierRepository.findByEventIdOrderByIdAsc(event.getId()).stream()
                .filter(item -> "GENERAL".equals(item.getName()))
                .findFirst()
                .orElseGet(TicketTier::new);
        tier.setEvent(event);
        tier.setName("GENERAL");
        tier.setPrice(100000.0);
        tier.setTotalQuantity(200);
        tier.setSoldQuantity(Math.max(tier.getSoldQuantity() == null ? 0 : tier.getSoldQuantity(), 1));
        tier.setDescription("General admission");
        return ticketTierRepository.save(tier);
    }

    private void upsertCheckinTicket(User user, Event event, TicketTier tier) {
        Ticket ticket = ticketRepository.findByTicketCode(checkinTicketCode).orElseGet(Ticket::new);
        Booking booking = ticket.getBooking();
        if (booking == null) {
            booking = new Booking();
            booking.setUser(user);
            booking.setEvent(event);
            booking.setTier(tier);
            booking.setQuantity(1);
            booking.setTotalPrice(tier.getPrice());
            booking.setBookingDate(LocalDateTime.now().minusDays(1));
        }
        booking.setStatus("PAID");
        Booking savedBooking = bookingRepository.save(booking);

        ticket.setTicketCode(checkinTicketCode);
        ticket.setTicketType(tier.getName());
        ticket.setStatus("ACTIVE");
        ticket.setCheckedIn(false);
        ticket.setCheckedInAt(null);
        ticket.setSeatNumber(null);
        ticket.setBooking(savedBooking);
        ticket.setUser(user);
        ticket.setTier(tier);
        ticketRepository.save(ticket);
    }
}
