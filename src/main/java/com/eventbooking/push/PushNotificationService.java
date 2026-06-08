package com.eventbooking.push;

import com.eventbooking.dto.push.PushSubscriptionRequest;
import com.eventbooking.entity.User;

public interface PushNotificationService {
    String publicKey();
    void subscribe(String email, PushSubscriptionRequest request);
    void unsubscribe(String email, String endpoint);
    void sendToUser(User user, String title, String body);
}
