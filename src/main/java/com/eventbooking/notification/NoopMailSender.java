package com.eventbooking.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopMailSender implements MailSender {
    private static final Logger log = LoggerFactory.getLogger(NoopMailSender.class);

    @Override
    public void send(EmailMessage message) {
        log.debug("email_disabled to={} subject={}", message.to(), message.subject());
    }
}
