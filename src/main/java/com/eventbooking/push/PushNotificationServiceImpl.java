package com.eventbooking.push;

import com.eventbooking.dto.push.PushSubscriptionRequest;
import com.eventbooking.entity.PushSubscription;
import com.eventbooking.entity.User;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.PushSubscriptionRepository;
import com.eventbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {
    private static final Logger log = LoggerFactory.getLogger(PushNotificationServiceImpl.class);

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    @Value("${app.push.vapid-public-key:}")
    private String vapidPublicKey;

    @Value("${app.push.vapid-private-key:}")
    private String vapidPrivateKey;

    @Override
    public String publicKey() {
        return vapidPublicKey == null ? "" : vapidPublicKey;
    }

    @Override
    @Transactional
    public void subscribe(String email, PushSubscriptionRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
        PushSubscription subscription = pushSubscriptionRepository.findByEndpoint(request.getEndpoint())
                .orElseGet(PushSubscription::new);
        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getKeys().getP256dh());
        subscription.setAuth(request.getKeys().getAuth());
        subscription.setUser(user);
        pushSubscriptionRepository.save(subscription);
    }

    @Override
    @Transactional
    public void unsubscribe(String email, String endpoint) {
        if (endpoint != null && !endpoint.isBlank()) {
            pushSubscriptionRepository.deleteByUserEmailAndEndpoint(email, endpoint);
        }
    }

    @Override
    public void sendToUser(User user, String title, String body) {
        if (user == null || vapidPublicKey == null || vapidPublicKey.isBlank()
                || vapidPrivateKey == null || vapidPrivateKey.isBlank()) {
            return;
        }
        String payload = "{\"title\":\"" + escape(title) + "\",\"body\":\"" + escape(body) + "\"}";
        for (PushSubscription subscription : pushSubscriptionRepository.findByUserEmail(user.getEmail())) {
            try {
                PushService pushService = new PushService(vapidPublicKey, vapidPrivateKey);
                pushService.send(new Notification(
                        subscription.getEndpoint(),
                        subscription.getP256dh(),
                        subscription.getAuth(),
                        payload
                ));
            } catch (Exception ex) {
                log.warn("push_notification_failed userId={} endpoint={}", user.getId(), subscription.getEndpoint());
            }
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
