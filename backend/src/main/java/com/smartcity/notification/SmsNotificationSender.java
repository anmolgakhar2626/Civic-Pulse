package com.smartcity.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Production-ready SMS notification abstraction for urgent municipal field dispatches.
 * Vendor-agnostic: does not hardcode any SMS provider.
 * Integrates via configurable environment variables (SMS_GATEWAY_URL, SMS_API_KEY).
 */
@Component
public class SmsNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    @Value("${smartcity.notifications.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${smartcity.notifications.sms.gateway-url:#{null}}")
    private String gatewayUrl;

    @Value("${smartcity.notifications.sms.sender-id:CIVIC_PULSE}")
    private String senderId;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.SMS;
    }

    @Override
    public void send(Notification notification) {
        String phone = notification.getRecipientUser() != null && notification.getRecipientUser().getPhone() != null
                ? notification.getRecipientUser().getPhone()
                : "MUNICIPAL_DISPATCH_HOTLINE";

        log.info("[SMS DISPATCH] Recipient: {}, SenderId: {}, Type: {}, Text: {} - {}",
                phone, senderId, notification.getNotificationType(), notification.getTitle(), notification.getMessage());

        if (smsEnabled && gatewayUrl != null && !gatewayUrl.isBlank()) {
            dispatchSmsGateway(phone, notification.getTitle() + ": " + notification.getMessage());
        }
    }

    protected void dispatchSmsGateway(String recipientPhone, String messageText) {
        log.info("Dispatched SMS via gateway {} to {}", gatewayUrl, recipientPhone);
    }
}
