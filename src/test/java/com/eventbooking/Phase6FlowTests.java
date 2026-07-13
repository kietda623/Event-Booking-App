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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Phase6FlowTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

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

    private ExecutorService executor;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("set referential_integrity false");
        safeDelete("delete from seats");
        safeDelete("delete from ticket_tiers");
        safeDelete("delete from refunds");
        safeDelete("delete from push_subscriptions");
        ticketRepository.deleteAll();
        paymentRepository.deleteAll();
        favoriteRepository.deleteAll();
        reminderRepository.deleteAll();
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("set referential_integrity true");
    }

    @AfterEach
    void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void nearbyEventsUseRadiusSortDistanceAndPublicPreview() throws Exception {
        saveEvent("Downtown Showcase", 10.7769, 106.7009);
        saveEvent("District Workshop", 10.7869, 106.7069);
        saveEvent("River Concert", 10.8069, 106.7209);
        saveEvent("Campus Meetup", 10.8569, 106.7809);
        saveEvent("Hanoi Forum", 21.0278, 105.8342);

        mockMvc.perform(get("/api/events")
                        .param("type", "nearby")
                        .param("latitude", "10.7769")
                        .param("longitude", "106.7009")
                        .param("radius", "10")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Downtown Showcase"))
                .andExpect(jsonPath("$.data.content[0].distanceKm").value(closeTo(0.0, 0.1)))
                .andExpect(jsonPath("$.data.content[*].title").value(not(hasItem("Hanoi Forum"))));

        mockMvc.perform(get("/api/events")
                        .param("type", "nearby")
                        .param("latitude", "10.7769")
                        .param("longitude", "106.7009")
                        .param("radius", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RADIUS"));

        mockMvc.perform(get("/api/events")
                        .param("type", "nearby"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_LOCATION_PARAMS"));

        mockMvc.perform(get("/api/events/nearby-preview")
                        .param("latitude", "10.7769")
                        .param("longitude", "106.7009"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].title").value("Downtown Showcase"))
                .andExpect(jsonPath("$.data[0].distanceKm").value(closeTo(0.0, 0.1)));
    }

    @Test
    void adminTierLifecycleAndBookingUseTierPriceAndAvailability() throws Exception {
        String adminToken = createAdminAndLogin();
        Event event = saveEvent("Tiered Event", 10.7769, 106.7009);
        Long vipTierId = createTier(adminToken, event.getId(), "VIP", 30.0, 3);
        Long generalTierId = createTier(adminToken, event.getId(), "General", 12.0, 5);
        String buyerToken = registerAndToken("Tier Buyer", "tier-buyer@example.com");

        mockMvc.perform(get("/api/events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tiers", hasSize(2)))
                .andExpect(jsonPath("$.data.tiers[0].availableQuantity").value(3));

        String bookingJson = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "eventId", event.getId(),
                                "tierId", vipTierId,
                                "quantity", 2
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalPrice").value(60.0))
                .andExpect(jsonPath("$.data.tierId").value(vipTierId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long bookingId = objectMapper.readTree(bookingJson).at("/data/bookingId").asLong();
        pay(buyerToken, bookingId);

        mockMvc.perform(put("/api/events/" + event.getId() + "/tiers/" + vipTierId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "VIP",
                                "price", 30.0,
                                "totalQuantity", 1,
                                "description", "Front section"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TIER_QUANTITY_BELOW_SOLD"));

        mockMvc.perform(delete("/api/events/" + event.getId() + "/tiers/" + vipTierId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TIER_ALREADY_SOLD"));

        mockMvc.perform(delete("/api/events/" + event.getId() + "/tiers/" + generalTierId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void onlyOneConcurrentBookingSucceedsWhenOneTierTicketLeft() throws Exception {
        String adminToken = createAdminAndLogin();
        Event event = saveEvent("Hot Tier Event", 10.7769, 106.7009);
        Long tierId = createTier(adminToken, event.getId(), "Last Seat", 99.0, 1);
        String firstToken = registerAndToken("First Tier Buyer", "first-tier@example.com");
        String secondToken = registerAndToken("Second Tier Buyer", "second-tier@example.com");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        executor = Executors.newFixedThreadPool(2);

        Future<Integer> first = executor.submit(() -> bookTierWhenReleased(firstToken, event.getId(), tierId, ready, start));
        Future<Integer> second = executor.submit(() -> bookTierWhenReleased(secondToken, event.getId(), tierId, ready, start));

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Integer> statuses = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        Integer sold = jdbcTemplate.queryForObject(
                "select sold_quantity from ticket_tiers where id = ?",
                Integer.class,
                tierId
        );
        assertThat(sold).isEqualTo(1);
    }

    @Test
    void usersCanHoldReleaseAndPayHeldSeats() throws Exception {
        String adminToken = createAdminAndLogin();
        Event event = saveEvent("Seat Map Event", 10.7769, 106.7009);
        Long tierId = createTier(adminToken, event.getId(), "Floor", 45.0, 3);
        seedSeat(event.getId(), tierId, "A1", "A", 1, "AVAILABLE", null, null);
        seedSeat(event.getId(), tierId, "A2", "A", 2, "AVAILABLE", null, null);
        seedSeat(event.getId(), tierId, "A3", "A", 3, "AVAILABLE", null, null);
        String buyerToken = registerAndToken("Seat Buyer", "seat-buyer@example.com");
        String otherToken = registerAndToken("Seat Other", "seat-other@example.com");

        mockMvc.perform(get("/api/events/" + event.getId() + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].seatNumber").value("A1"))
                .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"));

        holdSeats(buyerToken, event.getId(), List.of("A1", "A2"));

        mockMvc.perform(post("/api/events/" + event.getId() + "/seats/hold")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("seatNumbers", List.of("A1")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_NOT_AVAILABLE"));

        mockMvc.perform(delete("/api/events/" + event.getId() + "/seats/hold")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        holdSeats(buyerToken, event.getId(), List.of("A1", "A2"));
        String bookingJson = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "eventId", event.getId(),
                                "tierId", tierId,
                                "quantity", 2,
                                "seatNumbers", List.of("A1", "A2")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.seatNumbers", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long bookingId = objectMapper.readTree(bookingJson).at("/data/bookingId").asLong();
        pay(buyerToken, bookingId);

        Integer bookedSeats = jdbcTemplate.queryForObject(
                "select count(*) from seats where event_id = ? and status = 'BOOKED'",
                Integer.class,
                event.getId()
        );
        assertThat(bookedSeats).isEqualTo(2);
        assertThat(ticketRepository.findByBookingId(bookingId))
                .extracting("seatNumber")
                .containsExactlyInAnyOrder("A1", "A2");
    }

    @Test
    void seatExpirySchedulerReleasesExpiredHeldSeats() throws Exception {
        Event event = saveEvent("Expiry Event", 10.7769, 106.7009);
        Long tierId = saveTierDirect(event.getId(), "Timed", 20.0, 2);
        Long userId = saveUserDirect("Held User", "held-user@example.com");
        seedSeat(event.getId(), tierId, "B1", "B", 1, "HELD", LocalDateTime.now().minusMinutes(1), userId);
        seedSeat(event.getId(), tierId, "B2", "B", 2, "HELD", LocalDateTime.now().plusMinutes(5), userId);

        Object scheduler = applicationContext.getBean("eventSeatHoldScheduler");
        scheduler.getClass().getMethod("releaseExpiredHolds").invoke(scheduler);

        String expiredStatus = jdbcTemplate.queryForObject(
                "select status from seats where seat_number = 'B1'",
                String.class
        );
        String activeStatus = jdbcTemplate.queryForObject(
                "select status from seats where seat_number = 'B2'",
                String.class
        );
        assertThat(expiredStatus).isEqualTo("AVAILABLE");
        assertThat(activeStatus).isEqualTo("HELD");
    }

    private void holdSeats(String token, Long eventId, List<String> seatNumbers) throws Exception {
        mockMvc.perform(post("/api/events/" + eventId + "/seats/hold")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("seatNumbers", seatNumbers))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(seatNumbers.size())))
                .andExpect(jsonPath("$.data[0].status").value("HELD"))
                .andExpect(jsonPath("$.data[0].heldUntil").isString());
    }

    private int bookTierWhenReleased(String token, Long eventId, Long tierId,
                                     CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "eventId", eventId,
                                "tierId", tierId,
                                "quantity", 1
                        ))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private Long createTier(String adminToken, Long eventId, String name, double price, int totalQuantity) throws Exception {
        String tierJson = mockMvc.perform(post("/api/events/" + eventId + "/tiers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", name,
                                "price", price,
                                "totalQuantity", totalQuantity,
                                "description", name + " tickets"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.availableQuantity").value(totalQuantity))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(tierJson).at("/data/id").asLong();
    }

    private void seedSeat(Long eventId, Long tierId, String seatNumber, String row, int col,
                          String status, LocalDateTime heldUntil, Long heldByUserId) {
        jdbcTemplate.update("""
                insert into seats(event_id, tier_id, seat_number, seat_row, seat_col, status, held_until, held_by_user_id)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, eventId, tierId, seatNumber, row, col, status, heldUntil, heldByUserId);
    }

    private Long saveTierDirect(Long eventId, String name, double price, int totalQuantity) {
        jdbcTemplate.update("""
                insert into ticket_tiers(event_id, name, price, total_quantity, sold_quantity, description, created_at)
                values (?, ?, ?, ?, 0, ?, ?)
                """, eventId, name, price, totalQuantity, name + " tickets", LocalDateTime.now());
        return jdbcTemplate.queryForObject("select id from ticket_tiers where event_id = ? and name = ?", Long.class, eventId, name);
    }

    private Long saveUserDirect(String fullName, String email) {
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER")));
        User user = new User();
        user.setUsername(email);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRoles(new HashSet<>(Set.of(userRole)));
        return userRepository.save(user).getId();
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

    private String pay(String token, Long bookingId) throws Exception {
        return mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId, "method", "MOCK"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private Event saveEvent(String title, Double latitude, Double longitude) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(title + " description");
        event.setLocation("HCMC");
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setTotalTickets(100);
        event.setTicketPrice(10.0);
        event.setImageUrl("https://cdn.example.com/" + title.toLowerCase().replace(" ", "-") + ".jpg");
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        return eventRepository.save(event);
    }

    private void safeDelete(String statement) {
        try {
            jdbcTemplate.execute(statement);
        } catch (DataAccessException ignored) {
            // Phase-specific tables may not exist until the implementation is present.
        }
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
