package com.smartcity.notification;

import com.smartcity.websocket.WebSocketEventPublisher;
import com.smartcity.websocket.dto.AlertEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationSender.class);

    private final WebSocketEventPublisher webSocketEventPublisher;

    @Autowired
    public InAppNotificationSender(@Autowired(required = false) WebSocketEventPublisher webSocketEventPublisher) {
        this.webSocketEventPublisher = webSocketEventPublisher;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {
        log.info("Sending IN_APP notification: id={}, title={}, type={}",
                notification.getId(), notification.getTitle(), notification.getNotificationType());

        if (webSocketEventPublisher != null) {
            String severity = "MEDIUM";
            if (notification.getNotificationType() != null) {
                if (notification.getNotificationType().contains("CRITICAL") || notification.getNotificationType().contains("BREACHED")) {
                    severity = "CRITICAL";
                } else if (notification.getNotificationType().contains("RISK") || notification.getNotificationType().contains("ESCALATION")) {
                    severity = "HIGH";
                }
            }

            AlertEvent alertEvent = new AlertEvent(
                    notification.getNotificationType(),
                    severity,
                    notification.getTitle(),
                    notification.getMessage(),
                    notification.getId() != null ? notification.getId().toString() : null,
                    null,
                    null
            );
            webSocketEventPublisher.publishAlert(alertEvent);
        }
    }
}
