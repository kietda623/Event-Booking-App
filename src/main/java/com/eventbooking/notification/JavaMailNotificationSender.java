package com.eventbooking.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnBean(JavaMailSender.class)
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true")
public class JavaMailNotificationSender implements MailSender {
    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.html(), true);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Could not prepare email", ex);
        }
    }
}
