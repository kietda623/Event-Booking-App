package com.eventbooking;

import com.eventbooking.entity.Event;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventBookingFlowTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerPersistsUserAndReturnsJwt() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "alice",
                                "password", "password123",
                                "email", "alice@example.com"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username").value("alice"));

        assertThat(userRepository.findByUsername("alice")).isPresent();
    }

    @Test
    void userCanBookPayAndSeeTicket() throws Exception {
        Event event = new Event();
        event.setTitle("Conference");
        event.setDescription("Developer conference");
        event.setLocation("HCMC");
        event.setEventDate(LocalDateTime.now().plusDays(7));
        event.setTotalTickets(10);
        event.setTicketPrice(25.0);
        Event savedEvent = eventRepository.save(event);

        String token = registerAndToken("bob");

        mockMvc.perform(get("/api/events/" + savedEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Conference"))
                .andExpect(jsonPath("$.availableTickets").value(10));

        String bookingJson = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "eventId", savedEvent.getId(),
                                "quantity", 2
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(savedEvent.getId()))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(50.0))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long bookingId = objectMapper.readTree(bookingJson).get("bookingId").asLong();

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId))
                .andExpect(jsonPath("$.amount").value(50.0))
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventTitle").value("Conference"))
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].status").value("PAID"));
    }

    @Test
    void authAndSecurityFailuresReturnControlledErrors() throws Exception {
        registerAndToken("carol");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "carol",
                                "password", "password123",
                                "email", "carol-copy@example.com"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "carol-copy",
                                "password", "password123",
                                "email", "carol@example.com"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already exists"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "carol",
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        String userToken = registerAndToken("dave");
        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest("User Event", LocalDateTime.now().plusDays(3))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void adminCanCreateUpdateAndDeleteEvents() throws Exception {
        String adminToken = loginAndToken("admin", "admin123");

        String createdJson = mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest("Admin Event", LocalDateTime.now().plusDays(5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Admin Event"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long eventId = objectMapper.readTree(createdJson).get("id").asLong();

        mockMvc.perform(put("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequest("Updated Admin Event", LocalDateTime.now().plusDays(6))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Admin Event"));

        mockMvc.perform(delete("/api/events/" + eventId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void eventsSupportPaginationSearchSortAndUpcomingFilter() throws Exception {
        Event oldEvent = new Event();
        oldEvent.setTitle("Past Meetup");
        oldEvent.setDescription("Past");
        oldEvent.setLocation("Hanoi");
        oldEvent.setEventDate(LocalDateTime.now().minusDays(2));
        oldEvent.setTotalTickets(20);
        oldEvent.setTicketPrice(10.0);
        eventRepository.save(oldEvent);

        Event futureEvent = new Event();
        futureEvent.setTitle("Future Meetup");
        futureEvent.setDescription("Future");
        futureEvent.setLocation("HCMC");
        futureEvent.setEventDate(LocalDateTime.now().plusDays(2));
        futureEvent.setTotalTickets(20);
        futureEvent.setTicketPrice(15.0);
        eventRepository.save(futureEvent);

        mockMvc.perform(get("/api/events")
                        .param("search", "Meetup")
                        .param("upcoming", "true")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sortBy", "eventDate")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Future Meetup"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void bookingValidationFailuresAreControlled() throws Exception {
        Event event = new Event();
        event.setTitle("Small Event");
        event.setDescription("Small");
        event.setLocation("HCMC");
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setTotalTickets(1);
        event.setTicketPrice(12.0);
        Event savedEvent = eventRepository.save(event);
        String token = registerAndToken("ivy");

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventId", savedEvent.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(1))
                .andExpect(jsonPath("$.totalPrice").value(12.0));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "eventId", savedEvent.getId(),
                                "quantity", 0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Quantity must be at least 1"));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "eventId", 999999L,
                                "quantity", 1
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found"));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "eventId", savedEvent.getId(),
                                "quantity", 2
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Not enough tickets available"));
    }

    @Test
    void userCanViewProfileAndToggleFavorites() throws Exception {
        Event event = new Event();
        event.setTitle("Favorite Conference");
        event.setDescription("Favorite");
        event.setLocation("Da Nang");
        event.setEventDate(LocalDateTime.now().plusDays(4));
        event.setTotalTickets(10);
        event.setTicketPrice(30.0);
        Event savedEvent = eventRepository.save(event);
        String token = registerAndToken("erin");

        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("erin"));

        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "fullName", "Erin Nguyen",
                                "avatar", "https://example.com/erin.png"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Erin Nguyen"))
                .andExpect(jsonPath("$.avatar").value("https://example.com/erin.png"));

        mockMvc.perform(put("/api/users/reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventReminder", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventReminder").value(true));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/favorites/" + savedEvent.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(savedEvent.getId()))
                .andExpect(jsonPath("$.favorited").value(true));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventTitle").value("Favorite Conference"));

        mockMvc.perform(post("/api/favorites/" + savedEvent.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorited").value(false));

        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/favorites/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found"));
    }

    @Test
    void userCanCancelOnlyOwnPendingBooking() throws Exception {
        Event event = new Event();
        event.setTitle("Cancelable Event");
        event.setDescription("Cancelable");
        event.setLocation("HCMC");
        event.setEventDate(LocalDateTime.now().plusDays(8));
        event.setTotalTickets(10);
        event.setTicketPrice(20.0);
        Event savedEvent = eventRepository.save(event);

        String ownerToken = registerAndToken("frank");
        String otherToken = registerAndToken("grace");
        Long bookingId = createBooking(ownerToken, savedEvent.getId(), 1);

        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found"));

        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void paidBookingCannotBeCancelled() throws Exception {
        Event event = new Event();
        event.setTitle("Paid Event");
        event.setDescription("Paid");
        event.setLocation("HCMC");
        event.setEventDate(LocalDateTime.now().plusDays(9));
        event.setTotalTickets(10);
        event.setTicketPrice(20.0);
        Event savedEvent = eventRepository.save(event);

        String token = registerAndToken("henry");
        Long bookingId = createBooking(token, savedEvent.getId(), 1);
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only pending bookings can be cancelled"));
    }

    @Test
    void paymentRejectsOtherUsersCancelledAndAlreadyPaidBookings() throws Exception {
        Event event = new Event();
        event.setTitle("Payment Rules Event");
        event.setDescription("Payment rules");
        event.setLocation("HCMC");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setTotalTickets(10);
        event.setTicketPrice(20.0);
        Event savedEvent = eventRepository.save(event);

        String ownerToken = registerAndToken("jack");
        String otherToken = registerAndToken("kate");
        Long bookingId = createBooking(ownerToken, savedEvent.getId(), 1);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found"));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Booking is already paid"));

        Long cancelledBookingId = createBooking(ownerToken, savedEvent.getId(), 1);
        mockMvc.perform(post("/api/bookings/" + cancelledBookingId + "/cancel")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", cancelledBookingId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cancelled bookings cannot be paid"));
    }

    private String registerAndToken(String username) throws Exception {
        String authJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", "password123",
                                "email", username + "@example.com"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode auth = objectMapper.readTree(authJson);
        return auth.get("token").asText();
    }

    private String loginAndToken(String username, String password) throws Exception {
        String authJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode auth = objectMapper.readTree(authJson);
        return auth.get("token").asText();
    }

    private Long createBooking(String token, Long eventId, int quantity) throws Exception {
        String bookingJson = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "eventId", eventId,
                                "quantity", quantity
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(bookingJson).get("bookingId").asLong();
    }

    private String eventRequest(String title, LocalDateTime eventDate) throws Exception {
        return json(Map.of(
                "title", title,
                "description", title + " description",
                "eventDate", eventDate.toString(),
                "location", "HCMC",
                "price", 25.0,
                "totalTickets", 100
        ));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
