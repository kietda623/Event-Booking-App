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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Phase2FlowTests {
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

    @Test
    void adminCanCreateEventWithMediaCoordinatesAndAuditFields() throws Exception {
        String adminToken = createAdminAndLogin();

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest("Gallery Night", LocalDateTime.now().plusDays(5), 80,
                                "https://cdn.example.com/gallery.jpg", 10.7769, 106.7009)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Gallery Night"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://cdn.example.com/gallery.jpg"))
                .andExpect(jsonPath("$.data.latitude").value(10.7769))
                .andExpect(jsonPath("$.data.longitude").value(106.7009))
                .andExpect(jsonPath("$.data.createdAt").isString())
                .andExpect(jsonPath("$.data.updatedAt").isString());
    }

    @Test
    void eventFiltersSupportPopularUpcomingNearbyAndMissingLocationError() throws Exception {
        Event past = saveEvent("Past Meetup", LocalDateTime.now().minusDays(2), 10, 10.0, 21.0278, 105.8342);
        Event near = saveEvent("Near Workshop", LocalDateTime.now().plusDays(1), 10, 10.0, 10.7769, 106.7009);
        Event far = saveEvent("Far Workshop", LocalDateTime.now().plusDays(2), 10, 10.0, 21.0278, 105.8342);
        Event popular = saveEvent("Popular Concert", LocalDateTime.now().plusDays(3), 10, 20.0, 10.78, 106.7);

        String firstToken = registerAndToken("First Fan", "first-fan@example.com");
        String secondToken = registerAndToken("Second Fan", "second-fan@example.com");
        pay(firstToken, createBooking(firstToken, popular.getId(), 2));
        pay(secondToken, createBooking(secondToken, near.getId(), 1));

        mockMvc.perform(get("/api/events")
                        .param("type", "popular")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Popular Concert"));

        mockMvc.perform(get("/api/events")
                        .param("type", "upcoming")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Near Workshop"))
                .andExpect(jsonPath("$.data.content[*].title").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Past Meetup"))));

        mockMvc.perform(get("/api/events")
                        .param("type", "nearby")
                        .param("latitude", "10.7769")
                        .param("longitude", "106.7009")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Near Workshop"))
                .andExpect(jsonPath("$.data.content[*].title").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Far Workshop"))));

        mockMvc.perform(get("/api/events")
                        .param("type", "nearby"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_LOCATION_PARAMS"));
    }

    @Test
    void bookingsAndTicketsArePagedAndTicketsExposeOptionalFields() throws Exception {
        Event event = saveEvent("Paged Event", LocalDateTime.now().plusDays(4), 10, 15.0, 10.7769, 106.7009);
        String token = registerAndToken("Paged User", "paged@example.com");
        pay(token, createBooking(token, event.getId(), 1));
        pay(token, createBooking(token, event.getId(), 1));

        mockMvc.perform(get("/api/bookings/my")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].ticketType").value("GENERAL"))
                .andExpect(jsonPath("$.data.content[0].seatNumber").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void adminCanViewBookingsByEventAndMissingEventReturnsEventCode() throws Exception {
        String adminToken = createAdminAndLogin();
        Event event = saveEvent("Admin Booking Event", LocalDateTime.now().plusDays(6), 10, 30.0, 10.7769, 106.7009);
        String token = registerAndToken("Buyer Name", "buyer@example.com");
        createBooking(token, event.getId(), 2);

        mockMvc.perform(get("/api/events/" + event.getId() + "/bookings")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").isNumber())
                .andExpect(jsonPath("$.data.content[0].userName").value("Buyer Name"))
                .andExpect(jsonPath("$.data.content[0].quantity").value(2))
                .andExpect(jsonPath("$.data.content[0].totalPrice").value(60.0))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.content[0].createdAt").isString());

        mockMvc.perform(get("/api/events/999999/bookings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }

    @Test
    void bookingAndPaymentErrorsUsePhase2Codes() throws Exception {
        Event event = saveEvent("Error Codes Event", LocalDateTime.now().plusDays(7), 1, 40.0, 10.7769, 106.7009);
        String ownerToken = registerAndToken("Owner", "owner@example.com");
        String otherToken = registerAndToken("Other", "other@example.com");

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", event.getId(), "quantity", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_QUANTITY"));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", 999999L, "quantity", 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));

        Long bookingId = createBooking(ownerToken, event.getId(), 1);

        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_OWNED"));

        mockMvc.perform(post("/api/bookings/999999/cancel")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));

        pay(ownerToken, bookingId);

        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_PENDING"));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId, "method", "CREDIT_CARD"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOKING_ALREADY_PAID"));

        mockMvc.perform(get("/api/payments/999999")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
    }

    @Test
    void swaggerUiAndYamlArePublicInTestProfile() throws Exception {
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/vnd.oai.openapi")));

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
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

    private Long pay(String token, Long bookingId) throws Exception {
        String paymentJson = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId, "method", "CREDIT_CARD"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode payment = objectMapper.readTree(paymentJson);
        return payment.at("/data/paymentId").asLong();
    }

    private Event saveEvent(String title, LocalDateTime eventDate, int totalTickets, double price, Double latitude, Double longitude) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(title + " description");
        event.setLocation("HCMC");
        event.setEventDate(eventDate);
        event.setTotalTickets(totalTickets);
        event.setTicketPrice(price);
        event.setImageUrl("https://cdn.example.com/" + title.toLowerCase().replace(" ", "-") + ".jpg");
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        return eventRepository.save(event);
    }

    private String eventRequest(String title, LocalDateTime eventDate, int totalTickets, String imageUrl,
                                Double latitude, Double longitude) throws Exception {
        return json(Map.of(
                "title", title,
                "description", title + " description",
                "eventDate", eventDate.toString(),
                "location", "HCMC",
                "price", 25.0,
                "totalTickets", totalTickets,
                "imageUrl", imageUrl,
                "latitude", latitude,
                "longitude", longitude
        ));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
