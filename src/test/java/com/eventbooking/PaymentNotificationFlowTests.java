package com.eventbooking;

import com.eventbooking.entity.Event;
import com.eventbooking.entity.Reminder;
import com.eventbooking.notification.EmailMessage;
import com.eventbooking.notification.MailSender;
import com.eventbooking.payment.StripePaymentClient;
import com.eventbooking.payment.StripePaymentIntentRequest;
import com.eventbooking.payment.StripePaymentIntentResult;
import com.eventbooking.payment.StripeWebhookEvent;
import com.eventbooking.payment.StripeWebhookVerifier;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.PaymentRepository;
import com.eventbooking.repository.ReminderRepository;
import com.eventbooking.repository.TicketRepository;
import com.eventbooking.reminder.EventReminderScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.stripe.secret-key=test",
        "app.stripe.webhook-secret=whsec_test",
        "app.mail.enabled=true",
        "app.mail.from=noreply@example.com"
})
@AutoConfigureMockMvc
class PaymentNotificationFlowTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private TestStripePaymentClient stripePaymentClient;

    @Autowired
    private TestStripeWebhookVerifier stripeWebhookVerifier;

    @Autowired
    private TestMailSender mailSender;

    @Autowired
    private EventReminderScheduler eventReminderScheduler;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("delete from push_subscriptions");
        jdbcTemplate.execute("delete from refunds");
        ticketRepository.deleteAll();
        paymentRepository.deleteAll();
        reminderRepository.deleteAll();
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        jdbcTemplate.execute("delete from refresh_tokens");
        jdbcTemplate.execute("delete from user_roles");
        jdbcTemplate.execute("delete from users");
        stripePaymentClient.clear();
        stripeWebhookVerifier.clear();
        mailSender.clear();
    }

    @Test
    void stripePaymentCreatesPaymentIntentWithBookingAmountAndReusesExistingIntent() throws Exception {
        Event event = saveEvent("Stripe Expo", 25.0, LocalDateTime.now().plusDays(5));
        String token = registerAndToken("Stripe Buyer", "stripe-buyer@example.com");
        Long bookingId = createBooking(token, event.getId(), 2);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId, "method", "STRIPE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").value(bookingId))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.clientSecret").value("cs_test_1"))
                .andExpect(jsonPath("$.data.paymentIntentId").value("pi_test_1"));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId, "method", "STRIPE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientSecret").value("cs_test_1"))
                .andExpect(jsonPath("$.data.paymentIntentId").value("pi_test_1"));

        assertThat(stripePaymentClient.requests()).hasSize(1);
        StripePaymentIntentRequest request = stripePaymentClient.requests().get(0);
        assertThat(request.amount()).isEqualTo(5000L);
        assertThat(request.currency()).isEqualTo("vnd");
        assertThat(request.metadata()).containsEntry("bookingId", String.valueOf(bookingId));
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus()).isEqualTo("PENDING");
        assertThat(ticketRepository.findByBookingId(bookingId)).isEmpty();
    }

    @Test
    void stripeWebhookSucceededMarksBookingPaidCreatesTicketAndSendsConfirmationEmail() throws Exception {
        Event event = saveEvent("Webhook Expo", 40.0, LocalDateTime.now().plusDays(5));
        String token = registerAndToken("Webhook Buyer", "webhook-buyer@example.com");
        Long bookingId = createBooking(token, event.getId(), 1);
        createStripeIntent(token, bookingId);
        mailSender.clear();
        stripeWebhookVerifier.next(new StripeWebhookEvent(
                "payment_intent.succeeded",
                "pi_test_1",
                Map.of("bookingId", String.valueOf(bookingId))
        ));

        mockMvc.perform(post("/api/payments/webhook")
                        .header("Stripe-Signature", "valid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"evt_test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus()).isEqualTo("PAID");
        assertThat(ticketRepository.findByBookingId(bookingId)).hasSize(1);
        assertThat(paymentRepository.findByPaymentIntentId("pi_test_1").orElseThrow().getStatus()).isEqualTo("PAID");
        assertThat(mailSender.messages())
                .anySatisfy(message -> {
                    assertThat(message.to()).isEqualTo("webhook-buyer@example.com");
                    assertThat(message.subject()).contains("Booking confirmed");
                    assertThat(message.html()).contains("Webhook Expo");
                });
    }

    @Test
    void registerPaidBookingAndPaidCancellationSendExpectedEmails() throws Exception {
        Event event = saveEvent("Mail Expo", 80.0, LocalDateTime.now().plusDays(5));
        String token = registerAndToken("Mail Buyer", "mail-buyer@example.com");

        assertThat(mailSender.messages())
                .anySatisfy(message -> {
                    assertThat(message.to()).isEqualTo("mail-buyer@example.com");
                    assertThat(message.subject()).contains("Welcome");
                    assertThat(message.html()).contains("Mail Buyer");
                });

        mailSender.clear();
        Long bookingId = createBooking(token, event.getId(), 2);
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId, "method", "MOCK"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        assertThat(mailSender.messages())
                .anySatisfy(message -> {
                    assertThat(message.to()).isEqualTo("mail-buyer@example.com");
                    assertThat(message.subject()).contains("Booking confirmed");
                    assertThat(message.html()).contains("Mail Expo");
                });

        mailSender.clear();
        mockMvc.perform(put("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertThat(mailSender.messages())
                .anySatisfy(message -> {
                    assertThat(message.to()).isEqualTo("mail-buyer@example.com");
                    assertThat(message.subject()).contains("Refund");
                    assertThat(message.html()).contains(String.valueOf(bookingId));
                    assertThat(message.html()).contains("160.0");
                });
    }

    @Test
    void reminderSchedulerSendsEmailToPaidTicketHoldersWhoEnabledReminders() throws Exception {
        Event event = saveEvent("Tomorrow Expo", 30.0, LocalDateTime.now().plusHours(12));
        String firstToken = registerAndToken("First Reminder", "first-reminder@example.com");
        String secondToken = registerAndToken("Second Reminder", "second-reminder@example.com");
        Long firstBookingId = createBooking(firstToken, event.getId(), 1);
        Long secondBookingId = createBooking(secondToken, event.getId(), 1);
        enableReminder(firstToken);
        enableReminder(secondToken);
        payMock(firstToken, firstBookingId);
        payMock(secondToken, secondBookingId);
        mailSender.clear();

        int sent = eventReminderScheduler.sendDailyReminders();

        assertThat(sent).isEqualTo(2);
        assertThat(mailSender.messages())
                .extracting(EmailMessage::to)
                .containsExactlyInAnyOrder("first-reminder@example.com", "second-reminder@example.com");
        assertThat(mailSender.messages()).allSatisfy(message -> {
            assertThat(message.subject()).contains("Reminder");
            assertThat(message.html()).contains("Tomorrow Expo");
        });
    }

    private void enableReminder(String token) throws Exception {
        mockMvc.perform(put("/api/users/reminders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("eventReminder", true))))
                .andExpect(status().isOk());
    }

    private void createStripeIntent(String token, Long bookingId) throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId, "method", "STRIPE"))))
                .andExpect(status().isOk());
    }

    private void payMock(String token, Long bookingId) throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bookingId", bookingId, "method", "MOCK"))))
                .andExpect(status().isOk());
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

    private Event saveEvent(String title, double price, LocalDateTime eventDate) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(title + " description");
        event.setLocation("HCMC");
        event.setEventDate(eventDate);
        event.setTotalTickets(100);
        event.setTicketPrice(price);
        event.setImageUrl("https://cdn.example.com/" + title.toLowerCase().replace(" ", "-") + ".jpg");
        return eventRepository.save(event);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @TestConfiguration
    static class TestDoubles {
        @Bean
        @Primary
        TestStripePaymentClient testStripePaymentClient() {
            return new TestStripePaymentClient();
        }

        @Bean
        @Primary
        TestStripeWebhookVerifier testStripeWebhookVerifier() {
            return new TestStripeWebhookVerifier();
        }

        @Bean
        @Primary
        TestMailSender testMailSender() {
            return new TestMailSender();
        }
    }

    static class TestStripePaymentClient implements StripePaymentClient {
        private final List<StripePaymentIntentRequest> requests = new ArrayList<>();

        @Override
        public StripePaymentIntentResult createPaymentIntent(StripePaymentIntentRequest request) {
            requests.add(request);
            int next = requests.size();
            return new StripePaymentIntentResult("pi_test_" + next, "cs_test_" + next);
        }

        List<StripePaymentIntentRequest> requests() {
            return requests;
        }

        void clear() {
            requests.clear();
        }
    }

    static class TestStripeWebhookVerifier implements StripeWebhookVerifier {
        private StripeWebhookEvent next;

        @Override
        public StripeWebhookEvent verify(String payload, String signatureHeader) {
            return next;
        }

        void next(StripeWebhookEvent event) {
            this.next = event;
        }

        void clear() {
            next = null;
        }
    }

    static class TestMailSender implements MailSender {
        private final List<EmailMessage> messages = new CopyOnWriteArrayList<>();

        @Override
        public void send(EmailMessage message) {
            messages.add(message);
        }

        List<EmailMessage> messages() {
            return messages;
        }

        void clear() {
            messages.clear();
        }
    }
}
