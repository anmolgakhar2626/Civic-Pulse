package com.smartcity.notification;

import com.smartcity.incident.Incident;
import com.smartcity.sla.SLARecord;
import com.smartcity.user.User;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationDTO createNotification(
            User recipient,
            String role,
            String title,
            String message,
            String type,
            NotificationChannel channel
    );

    NotificationDTO createNotification(
            User recipient,
            String role,
            String title,
            String message,
            String type,
            String severity,
            UUID incidentId,
            String dedupKey,
            NotificationChannel channel
    );

    NotificationDTO createComplaintNotification(
            User recipient,
            String role,
            String title,
            String message,
            String type,
            String severity,
            UUID complaintId,
            String complaintNumber,
            NotificationChannel channel
    );

    void notifyCriticalIncident(Incident incident, String departmentName);

    void notifySlaAtRisk(SLARecord slaRecord, String departmentName);

    void notifySlaBreached(SLARecord slaRecord, String departmentName);

    void notifyLevel3Escalation(SLARecord slaRecord, String reason);

    NotificationDTO getNotificationById(UUID id, UUID userId, List<String> roles);

    List<NotificationDTO> getNotificationsForUser(
            UUID userId,
            List<String> roles,
            Boolean unreadOnly,
            String type,
            String severity,
            int page,
            int size
    );

    List<NotificationDTO> getNotificationsForUser(
            UUID userId,
            List<String> roles,
            Boolean unreadOnly,
            String type,
            String severity,
            Boolean directOnly,
            int page,
            int size
    );

    long getUnreadCountForUser(UUID userId, List<String> roles);

    NotificationDTO markAsRead(UUID id, UUID userId);

    NotificationDTO markAsRead(UUID id, UUID userId, List<String> roles);

    NotificationDTO markAsUnread(UUID id, UUID userId, List<String> roles);

    int markAllAsRead(UUID userId, List<String> roles);

    boolean deleteNotification(UUID id, UUID userId);

    boolean deleteNotification(UUID id, UUID userId, List<String> roles);

    // Backward compatibility methods
    List<NotificationDTO> getNotificationsForUser(UUID userId, List<String> roles, Boolean unreadOnly);

    NotificationDTO markAsRead(UUID id);

    int markAllAsRead(UUID userId);
}
