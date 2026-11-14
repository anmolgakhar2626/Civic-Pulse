package com.smartcity.notification;

public interface NotificationSender {
    boolean supports(NotificationChannel channel);
    void send(Notification notification);
}
