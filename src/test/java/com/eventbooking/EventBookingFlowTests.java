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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventBookingFlowTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private ExecutorService executor;

    @BeforeEach
    void cleanDatabase() {
        ticketRepository.deleteAll();
        paymentRepository.deleteAll();
        favoriteRepository.deleteAll();
        reminderRepository.deleteAll();
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void registerAndLoginUseEmailFirstAndReturnAuthEnvelope() throws Exception {
        String authJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fullName", "Alice Nguyen",
                                "email", "alice@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.expiresAt").isString())
                .andExpect(jsonPath("$.data.user.id").isNumber())
                .andExpect(jsonPath("$.data.user.fullName").value("Alice Nguyen"))
                .andExpect(jsonPath("$.data.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.username").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
        assertThat(objectMapper.readTree(authJson).at("/data/accessToken").asText()).isNotBlank();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "alice@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.email").value("alice@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fullName", "Alice Duplicate",
                                "email", "alice@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.errors").isArray());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "alice@example.com",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void authSecurityFailuresUseErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.errors").isArray());

        String userToken = registerAndToken("Bob User", "bob@example.com");
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest("User Event", LocalDateTime.now().plusDays(3), 100)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void eventBookingPaymentTicketProfileAndFavoriteEndpointsUseResponseEnvelope() throws Exception {
        Event savedEvent = saveEvent("Conference", 10, 25.0);
        String token = registerAndToken("Carol Tran", "carol@example.com");

        mockMvc.perform(get("/api/events/" + savedEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Conference"))
                .andExpect(jsonPath("$.data.availableTickets").value(10));

        Long bookingId = createBooking(token, savedEvent.getId(), 2);

        mockMvc.perform(get("/api/bookings/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].bookingId").value(bookingId))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));

        String paymentJson = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "bookingId", bookingId,
                                "method", "CREDIT_CARD"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingId").value(bookingId))
                .andExpect(jsonPath("$.data.amount").value(50.0))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.ticketId").isNumber())
                .andExpect(jsonPath("$.data.ticketCode").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String ticketCode = objectMapper.readTree(paymentJson).at("/data/ticketCode").asText();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "bookingId", bookingId,
                                "method", "CREDIT_CARD"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BOOKING_ALREADY_PAID"));

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].ticketCode").value(ticketCode))
                .andExpect(jsonPath("$.data.content[0].eventTitle").value("Conference"))
                .andExpect(jsonPath("$.data.content[0].status").value("PAID"));

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Carol Tran"))
                .andExpect(jsonPath("$.data.email").value("carol@example.com"));

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fullName", "Carol Updated",
                                "avatar", "https://example.com/carol.png"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Carol Updated"));

        mockMvc.perform(put("/api/users/reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventReminder", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventReminder").value(true));

        mockMvc.perform(post("/api/favorites/" + savedEvent.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(savedEvent.getId()))
                .andExpect(jsonPath("$.data.favorited").value(true));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].eventTitle").value("Conference"));
    }

    @Test
    void adminEventCrudUsesEnvelopeIncludingDelete() throws Exception {
        String adminToken = createAdminAndLogin();

        String createdJson = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest("Admin Event", LocalDateTime.now().plusDays(5), 100)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Admin Event"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(createdJson).at("/data/id").asLong();

        mockMvc.perform(put("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest("Updated Admin Event", LocalDateTime.now().plusDays(6), 80)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Admin Event"));

        mockMvc.perform(delete("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void bookingRejectsSoldOutWithConflictCode() throws Exception {
        Event savedEvent = saveEvent("Small Event", 1, 12.0);
        String token = registerAndToken("Ivy Le", "ivy@example.com");

        createBooking(token, savedEvent.getId(), 1);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "eventId", savedEvent.getId(),
                                "quantity", 1
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("EVENT_SOLD_OUT"));
    }

    @Test
    void onlyOneConcurrentBookingSucceedsWhenOneTicketLeft() throws Exception {
        Event savedEvent = saveEvent("Hot Event", 1, 99.0);
        String firstToken = registerAndToken("First Buyer", "first@example.com");
        String secondToken = registerAndToken("Second Buyer", "second@example.com");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        executor = Executors.newFixedThreadPool(2);

        Future<Integer> first = executor.submit(() -> bookWhenReleased(firstToken, savedEvent.getId(), ready, start));
        Future<Integer> second = executor.submit(() -> bookWhenReleased(secondToken, savedEvent.getId(), ready, start));

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Integer> statuses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(bookingRepository.sumBookedQuantityByEventId(savedEvent.getId())).isEqualTo(1L);
    }

    private int bookWhenReleased(String token, Long eventId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "eventId", eventId,
                                "quantity", 1
                        ))))
                .andReturn()
                .getResponse()
                .getStatus();
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
        JsonNode auth = objectMapper.readTree(authJson);
        return auth.at("/data/accessToken").asText();
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
        JsonNode auth = objectMapper.readTree(authJson);
        return auth.at("/data/accessToken").asText();
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
                        .content(json(Map.of(
                                "eventId", eventId,
                                "quantity", quantity
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(bookingJson).at("/data/bookingId").asLong();
    }

    private Event saveEvent(String title, int totalTickets, double price) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(title + " description");
        event.setLocation("HCMC");
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setTotalTickets(totalTickets);
        event.setTicketPrice(price);
        return eventRepository.save(event);
    }

    private String eventRequest(String title, LocalDateTime eventDate, int totalTickets) throws Exception {
        return json(Map.of(
                "title", title,
                "description", title + " description",
                "eventDate", eventDate.toString(),
                "location", "HCMC",
                "price", 25.0,
                "totalTickets", totalTickets
        ));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
