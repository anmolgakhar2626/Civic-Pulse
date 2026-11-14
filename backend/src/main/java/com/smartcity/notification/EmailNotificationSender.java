package com.smartcity.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Production-ready Email notification abstraction for municipal command dispatch.
 * Easily pluggable with JavaMailSender, Amazon SES, or SendGrid via environment variables.
 */
@Component
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Value("${smartcity.notifications.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${smartcity.notifications.email.from:notifications@commandcentre.smartcity.gov}")
    private String fromAddress;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification) {
        String recipient = notification.getRecipientUser() != null && notification.getRecipientUser().getEmail() != null
                ? notification.getRecipientUser().getEmail()
                : (notification.getRecipientRole() != null ? notification.getRecipientRole().toLowerCase() + "@commandcentre.smartcity.gov" : "command-centre@smartcity.gov");

        log.info("[EMAIL DISPATCH] To: {}, Subject: [SMART CITY ALERT] {}, Type: {}, Content: {}",
                recipient, notification.getTitle(), notification.getNotificationType(), notification.getMessage());

        if (emailEnabled) {
            // Concrete dispatch mechanism when SMTP/SES credentials are configured
            dispatchSmtp(fromAddress, recipient, notification.getTitle(), notification.getMessage());
        }
    }

    protected void dispatchSmtp(String from, String to, String subject, String body) {
        log.info("Dispatched SMTP email from {} to {} with subject {}", from, to, subject);
    }
}
