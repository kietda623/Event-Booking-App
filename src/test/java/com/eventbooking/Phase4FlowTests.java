package com.eventbooking;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.Role;
import com.eventbooking.entity.User;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.FavoriteRepository;
import com.eventbooking.repository.PaymentRepository;
import com.eventbooking.repository.ReminderRepository;
import com.eventbooking.repository.RoleRepository;
import com.eventbooking.repository.TicketRepository;
import com.eventbooking.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Phase4FlowTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("delete from refunds");
        ticketRepository.deleteAll();
        paymentRepository.deleteAll();
        favoriteRepository.deleteAll();
        reminderRepository.deleteAll();
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void userCanAddListAndRemoveFavoritesWithPhase4Endpoints() throws Exception {
        Event event = saveEvent("Favorite Event", 50.0);
        String token = registerAndToken("Favorite User", "favorite@example.com");

        mockMvc.perform(post("/api/events/" + event.getId() + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value(event.getId()))
                .andExpect(jsonPath("$.data.favorited").value(true));

        mockMvc.perform(post("/api/events/" + event.getId() + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_FAVORITED"));

        mockMvc.perform(get("/api/users/favorites")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Favorite Event"))
                .andExpect(jsonPath("$.data.page").value(0));

        mockMvc.perform(delete("/api/events/" + event.getId() + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value(event.getId()))
                .andExpect(jsonPath("$.data.favorited").value(false));

        mockMvc.perform(delete("/api/events/" + event.getId() + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FAVORITE_NOT_FOUND"));
    }

    @Test
    void paidBookingCancelCreatesRefundAndCancelsTickets() throws Exception {
        Event event = saveEvent("Refund Event", 80.0);
        String token = registerAndToken("Refund User", "refund@example.com");
        Long bookingId = createBooking(token, event.getId(), 2);
        String paymentJson = pay(token, bookingId);
        String ticketCode = objectMapper.readTree(paymentJson).at("/data/ticketCode").asText();

        mockMvc.perform(put("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").value(bookingId))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refundStatus").value("PENDING"));

        Integer refunds = jdbcTemplate.queryForObject(
                "select count(*) from refunds where booking_id = ? and amount = ? and status = 'PENDING'",
                Integer.class,
                bookingId,
                160.0
        );
        assertThat(refunds).isEqualTo(1);

        mockMvc.perform(post("/api/tickets/checkin")
                        .header("Authorization", "Bearer " + createAdminAndLogin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticketCode", ticketCode))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_CANCELLED"));

        mockMvc.perform(put("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_CANCELLABLE"));
    }

    @Test
    void adminCanCheckInActiveTicketOnlyOnce() throws Exception {
        Event event = saveEvent("Check In Event", 35.0);
        String token = registerAndToken("Ticket User", "ticket@example.com");
        Long bookingId = createBooking(token, event.getId(), 1);
        String paymentJson = pay(token, bookingId);
        String ticketCode = objectMapper.readTree(paymentJson).at("/data/ticketCode").asText();
        String adminToken = createAdminAndLogin();

        mockMvc.perform(post("/api/tickets/checkin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticketCode", ticketCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketCode").value(ticketCode))
                .andExpect(jsonPath("$.data.checkedIn").value(true))
                .andExpect(jsonPath("$.data.checkedInAt").isString())
                .andExpect(jsonPath("$.data.attendeeName").value("Ticket User"))
                .andExpect(jsonPath("$.data.eventTitle").value("Check In Event"));

        mockMvc.perform(post("/api/tickets/checkin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticketCode", ticketCode))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_ALREADY_CHECKED_IN"));

        mockMvc.perform(post("/api/tickets/checkin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("ticketCode", "missing-ticket"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void adminAnalyticsReturnsSummaryStatusCountsAndTopEvents() throws Exception {
        Event first = saveEvent("Top Event", 20.0);
        Event second = saveEvent("Second Event", 15.0);
        String firstToken = registerAndToken("First Buyer", "first-analytics@example.com");
        String secondToken = registerAndToken("Second Buyer", "second-analytics@example.com");
        pay(firstToken, createBooking(firstToken, first.getId(), 3));
        pay(secondToken, createBooking(secondToken, second.getId(), 1));
        createBooking(secondToken, first.getId(), 1);

        mockMvc.perform(get("/api/admin/analytics")
                        .header("Authorization", "Bearer " + createAdminAndLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEvents").value(2))
                .andExpect(jsonPath("$.data.totalBookings").value(3))
                .andExpect(jsonPath("$.data.totalRevenue").value(75.0))
                .andExpect(jsonPath("$.data.bookingsByStatus.PENDING").value(1))
                .andExpect(jsonPath("$.data.bookingsByStatus.PAID").value(2))
                .andExpect(jsonPath("$.data.topEvents[0].id").value(first.getId()))
                .andExpect(jsonPath("$.data.topEvents[0].bookedCount").value(3));
    }

    private String registerAndToken(String fullName, String email) throws Exception {
        String authJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fullName", fullName,
                                "email", email,
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(authJson).at("/data/accessToken").asText();
    }

    private String loginAndToken(String email, String password) throws Exception {
        String authJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(authJson).at("/data/accessToken").asText();
    }

    private String createAdminAndLogin() throws Exception {
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER")));
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN")));
        User admin = new User();
        admin.setUsername("admin@example.com");
        admin.setFullName("Admin User");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRoles(new HashSet<>(Set.of(userRole, adminRole)));
        userRepository.save(admin);
        return loginAndToken("admin@example.com", "admin123");
    }

    private Long createBooking(String token, Long eventId, int quantity) throws Exception {
        String bookingJson = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", eventId, "quantity", quantity))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(bookingJson).at("/data/bookingId").asLong();
    }

    private String pay(String token, Long bookingId) throws Exception {
        return mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId, "method", "CREDIT_CARD"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private Event saveEvent(String title, double price) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(title + " description");
        event.setLocation("HCMC");
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setTotalTickets(100);
        event.setTicketPrice(price);
        event.setImageUrl("https://cdn.example.com/" + title.toLowerCase().replace(" ", "-") + ".jpg");
        return eventRepository.save(event);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
