package com.smartcity.notification;

import com.smartcity.notification.event.*;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import com.smartcity.websocket.WebSocketEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationDao notificationDao;
    private final UserRepository userRepository;
    private final WebSocketEventPublisher webSocketEventPublisher;

    public NotificationEventListener(
            NotificationDao notificationDao,
            UserRepository userRepository,
            WebSocketEventPublisher webSocketEventPublisher
    ) {
        this.notificationDao = notificationDao;
        this.userRepository = userRepository;
        this.webSocketEventPublisher = webSocketEventPublisher;
    }

    /**
     * 1. New Incident Created
     */
    @EventListener
    public void handleIncidentCreated(IncidentCreatedNotificationEvent event) {
        try {
            String dedupKey = "INC_CREATED_" + event.getIncidentId();
            if (notificationDao.existsByDedupKey(dedupKey)) {
                log.debug("Skipping duplicate notification for {}", dedupKey);
                return;
            }

            boolean isCriticalOrHigh = "CRITICAL".equalsIgnoreCase(event.getSeverity()) || "HIGH".equalsIgnoreCase(event.getSeverity());
            String title = (isCriticalOrHigh ? "URGENT INCIDENT: " : "NEW INCIDENT: ") + event.getIncidentNumber();
            String msg = String.format("A %s incident (%s) has been logged: %s. Department: %s",
                    event.getSeverity(), event.getCategory(), event.getTitle(),
                    event.getDepartmentName() != null ? event.getDepartmentName() : "General Dispatch");

            // Notification for Command Operators
            Notification opNotif = new Notification();
            opNotif.setIncidentId(event.getIncidentId());
            opNotif.setRecipientRole("COMMAND_OPERATOR");
            opNotif.setTitle(title);
            opNotif.setMessage(msg);
            opNotif.setNotificationType(isCriticalOrHigh ? NotificationType.CRITICAL_ALERT.name() : NotificationType.INCIDENT_CREATED.name());
            opNotif.setSeverity(event.getSeverity());
            opNotif.setDedupKey(dedupKey);
            opNotif.setSentAt(Instant.now());
            opNotif.setCreatedAt(Instant.now());
            NotificationDTO savedOp = notificationDao.save(opNotif);
            publishWebSocket(savedOp);

            // If critical, also notify ADMIN
            if ("CRITICAL".equalsIgnoreCase(event.getSeverity())) {
                Notification adminNotif = new Notification();
                adminNotif.setIncidentId(event.getIncidentId());
                adminNotif.setRecipientRole("ADMIN");
                adminNotif.setTitle(title);
                adminNotif.setMessage(msg);
                adminNotif.setNotificationType(NotificationType.CRITICAL_ALERT.name());
                adminNotif.setSeverity("CRITICAL");
                adminNotif.setDedupKey(dedupKey + "_ADMIN");
                adminNotif.setSentAt(Instant.now());
                adminNotif.setCreatedAt(Instant.now());
                NotificationDTO savedAdmin = notificationDao.save(adminNotif);
                publishWebSocket(savedAdmin);
            }

            // If pre-assigned to a specific operator upon creation, notify them directly
            if (event.getAssignedToUserId() != null) {
                userRepository.findById(event.getAssignedToUserId()).ifPresent(user -> {
                    Notification directNotif = new Notification();
                    directNotif.setIncidentId(event.getIncidentId());
                    directNotif.setRecipientUser(user);
                    directNotif.setTitle("Incident Assigned: " + event.getIncidentNumber());
                    directNotif.setMessage("You have been assigned to incident: " + event.getTitle());
                    directNotif.setNotificationType(NotificationType.INCIDENT_ASSIGNED.name());
                    directNotif.setSeverity(event.getSeverity());
                    directNotif.setDedupKey("INC_ASSIGNED_" + event.getIncidentId() + "_" + user.getId());
                    directNotif.setSentAt(Instant.now());
                    directNotif.setCreatedAt(Instant.now());
                    NotificationDTO savedDirect = notificationDao.save(directNotif);
                    publishWebSocket(savedDirect);
                });
            }
        } catch (Exception e) {
            log.error("Failed to handle IncidentCreatedNotificationEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * 2. Incident Assigned to Operator
     */
    @EventListener
    public void handleIncidentAssigned(IncidentAssignedNotificationEvent event) {
        try {
            if (event.getAssignedToUserId() == null) return;

            String dedupKey = "INC_ASSIGNED_" + event.getIncidentId() + "_" + event.getAssignedToUserId();
            if (notificationDao.existsByDedupKey(dedupKey)) {
                return;
            }

            userRepository.findById(event.getAssignedToUserId()).ifPresent(operator -> {
                Notification notif = new Notification();
                notif.setIncidentId(event.getIncidentId());
                notif.setRecipientUser(operator);
                notif.setTitle("Incident Assigned: " + event.getIncidentNumber());
                notif.setMessage(String.format("You have been assigned to incident '%s' by %s.",
                        event.getTitle(),
                        event.getAssignedByUsername() != null ? event.getAssignedByUsername() : "Command Staff"));
                notif.setNotificationType(NotificationType.INCIDENT_ASSIGNED.name());
                notif.setSeverity(event.getSeverity());
                notif.setDedupKey(dedupKey);
                notif.setSentAt(Instant.now());
                notif.setCreatedAt(Instant.now());
                NotificationDTO saved = notificationDao.save(notif);
                publishWebSocket(saved);
            });
        } catch (Exception e) {
            log.error("Failed to handle IncidentAssignedNotificationEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * 3. Incident Status Changed & Resolution
     */
    @EventListener
    public void handleIncidentStatusChanged(IncidentStatusChangedNotificationEvent event) {
        try {
            String dedupKey = "INC_STATUS_" + event.getIncidentId() + "_" + event.getNewStatus();
            if (notificationDao.existsByDedupKey(dedupKey)) {
                return;
            }

            boolean isResolved = "RESOLVED".equalsIgnoreCase(event.getNewStatus()) || "CLOSED".equalsIgnoreCase(event.getNewStatus());
            String notifType = isResolved ? NotificationType.INCIDENT_RESOLVED.name() : NotificationType.STATUS_CHANGED.name();
            String title = isResolved ? ("Incident Resolved: " + event.getIncidentNumber()) : ("Status Updated: " + event.getIncidentNumber());
            String msg = String.format("Status transitioned from %s to %s for incident '%s' by %s.",
                    event.getOldStatus() != null ? event.getOldStatus() : "OPEN",
                    event.getNewStatus(),
                    event.getTitle(),
                    event.getChangedByUsername() != null ? event.getChangedByUsername() : "Command Centre");

            // Notify assigned operator if changed by another party
            if (event.getAssignedToUserId() != null) {
                userRepository.findById(event.getAssignedToUserId()).ifPresent(op -> {
                    Notification notif = new Notification();
                    notif.setIncidentId(event.getIncidentId());
                    notif.setRecipientUser(op);
                    notif.setTitle(title);
                    notif.setMessage(msg);
                    notif.setNotificationType(notifType);
                    notif.setSeverity(isResolved ? "LOW" : "MEDIUM");
                    notif.setDedupKey(dedupKey + "_OP");
                    notif.setSentAt(Instant.now());
                    notif.setCreatedAt(Instant.now());
                    NotificationDTO saved = notificationDao.save(notif);
                    publishWebSocket(saved);
                });
            }

            // If resolved, notify the reporting citizen
            if (isResolved && event.getReportedByUserId() != null) {
                userRepository.findById(event.getReportedByUserId()).ifPresent(citizen -> {
                    Notification notif = new Notification();
                    notif.setIncidentId(event.getIncidentId());
                    notif.setRecipientUser(citizen);
                    notif.setTitle("Your Reported Incident Has Been Resolved");
                    notif.setMessage("Incident " + event.getIncidentNumber() + " ('" + event.getTitle() + "') was marked as RESOLVED.");
                    notif.setNotificationType(NotificationType.INCIDENT_RESOLVED.name());
                    notif.setSeverity("LOW");
                    notif.setDedupKey(dedupKey + "_CITIZEN");
                    notif.setSentAt(Instant.now());
                    notif.setCreatedAt(Instant.now());
                    NotificationDTO saved = notificationDao.save(notif);
                    publishWebSocket(saved);
                });
            }
        } catch (Exception e) {
            log.error("Failed to handle IncidentStatusChangedNotificationEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * 4. SLA Approaching Deadline (WARNING)
     */
    @EventListener
    public void handleSlaWarning(SlaWarningNotificationEvent event) {
        try {
            String dedupKey = "SLA_WARN_" + event.getSlaId();
            if (notificationDao.existsByDedupKey(dedupKey)) {
                return;
            }

            String title = "SLA WARNING: " + event.getIncidentNumber();
            String msg = String.format("Incident %s is approaching its resolution deadline. Remaining time: %d minutes. Target: %s.",
                    event.getIncidentNumber(), event.getRemainingMinutes(), event.getDeadlineAt());

            // 1. Notify Command Operators
            Notification opNotif = new Notification();
            opNotif.setIncidentId(event.getIncidentId());
            opNotif.setRecipientRole("COMMAND_OPERATOR");
            opNotif.setTitle(title);
            opNotif.setMessage(msg);
            opNotif.setNotificationType(NotificationType.SLA_WARNING.name());
            opNotif.setSeverity("HIGH");
            opNotif.setDedupKey(dedupKey);
            opNotif.setSentAt(Instant.now());
            opNotif.setCreatedAt(Instant.now());
            NotificationDTO savedOp = notificationDao.save(opNotif);
            publishWebSocket(savedOp);

            // 2. Notify assigned operator if assigned
            if (event.getAssignedToUserId() != null) {
                userRepository.findById(event.getAssignedToUserId()).ifPresent(op -> {
                    Notification directNotif = new Notification();
                    directNotif.setIncidentId(event.getIncidentId());
                    directNotif.setRecipientUser(op);
                    directNotif.setTitle(title);
                    directNotif.setMessage(msg);
                    directNotif.setNotificationType(NotificationType.SLA_WARNING.name());
                    directNotif.setSeverity("HIGH");
                    directNotif.setDedupKey(dedupKey + "_DIRECT");
                    directNotif.setSentAt(Instant.now());
                    directNotif.setCreatedAt(Instant.now());
                    NotificationDTO savedDirect = notificationDao.save(directNotif);
                    publishWebSocket(savedDirect);
                });
            }
        } catch (Exception e) {
            log.error("Failed to handle SlaWarningNotificationEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * 5. SLA Breached
     */
    @EventListener
    public void handleSlaBreached(SlaBreachedNotificationEvent event) {
        try {
            String dedupKey = "SLA_BREACH_" + event.getSlaId();
            if (notificationDao.existsByDedupKey(dedupKey)) {
                return;
            }

            String title = "SLA BREACHED: " + event.getIncidentNumber();
            String msg = String.format("Incident %s has breached its SLA resolution deadline. Department: %s. Immediate intervention required.",
                    event.getIncidentNumber(),
                    event.getDepartmentName() != null ? event.getDepartmentName() : "Assigned Dept");

            // 1. Notify Command Operators
            Notification opNotif = new Notification();
            opNotif.setIncidentId(event.getIncidentId());
            opNotif.setRecipientRole("COMMAND_OPERATOR");
            opNotif.setTitle(title);
            opNotif.setMessage(msg);
            opNotif.setNotificationType(NotificationType.SLA_BREACHED.name());
            opNotif.setSeverity("CRITICAL");
            opNotif.setDedupKey(dedupKey);
            opNotif.setSentAt(Instant.now());
            opNotif.setCreatedAt(Instant.now());
            NotificationDTO savedOp = notificationDao.save(opNotif);
            publishWebSocket(savedOp);

            // 2. Notify Admin
            Notification adminNotif = new Notification();
            adminNotif.setIncidentId(event.getIncidentId());
            adminNotif.setRecipientRole("ADMIN");
            adminNotif.setTitle(title);
            adminNotif.setMessage(msg);
            adminNotif.setNotificationType(NotificationType.SLA_BREACHED.name());
            adminNotif.setSeverity("CRITICAL");
            adminNotif.setDedupKey(dedupKey + "_ADMIN");
            adminNotif.setSentAt(Instant.now());
            adminNotif.setCreatedAt(Instant.now());
            NotificationDTO savedAdmin = notificationDao.save(adminNotif);
            publishWebSocket(savedAdmin);

            // 3. Notify assigned operator if assigned
            if (event.getAssignedToUserId() != null) {
                userRepository.findById(event.getAssignedToUserId()).ifPresent(op -> {
                    Notification directNotif = new Notification();
                    directNotif.setIncidentId(event.getIncidentId());
                    directNotif.setRecipientUser(op);
                    directNotif.setTitle(title);
                    directNotif.setMessage(msg);
                    directNotif.setNotificationType(NotificationType.SLA_BREACHED.name());
                    directNotif.setSeverity("CRITICAL");
                    directNotif.setDedupKey(dedupKey + "_DIRECT");
                    directNotif.setSentAt(Instant.now());
                    directNotif.setCreatedAt(Instant.now());
                    NotificationDTO savedDirect = notificationDao.save(directNotif);
                    publishWebSocket(savedDirect);
                });
            }
        } catch (Exception e) {
            log.error("Failed to handle SlaBreachedNotificationEvent: {}", e.getMessage(), e);
        }
    }

    /**
     * 6. Incident Escalated
     */
    @EventListener
    public void handleIncidentEscalated(IncidentEscalatedNotificationEvent event) {
        try {
            String dedupKey = "INC_ESC_" + event.getIncidentId() + "_" + event.getEscalationLevel();
            if (notificationDao.existsByDedupKey(dedupKey)) {
                return;
            }

            String title = "INCIDENT ESCALATED (" + event.getEscalationLevel() + "): " + event.getIncidentNumber();
            String msg = String.format("Incident %s has been escalated to %s. Reason: %s.",
                    event.getIncidentNumber(), event.getEscalationLevel(),
                    event.getReason() != null ? event.getReason() : "Breached operational deadline");

            // Notify Command Operators & Admin
            Notification opNotif = new Notification();
            opNotif.setIncidentId(event.getIncidentId());
            opNotif.setRecipientRole("COMMAND_OPERATOR");
            opNotif.setTitle(title);
            opNotif.setMessage(msg);
            opNotif.setNotificationType(NotificationType.INCIDENT_ESCALATED.name());
            opNotif.setSeverity("CRITICAL");
            opNotif.setDedupKey(dedupKey);
            opNotif.setSentAt(Instant.now());
            opNotif.setCreatedAt(Instant.now());
            NotificationDTO savedOp = notificationDao.save(opNotif);
            publishWebSocket(savedOp);

            Notification adminNotif = new Notification();
            adminNotif.setIncidentId(event.getIncidentId());
            adminNotif.setRecipientRole("ADMIN");
            adminNotif.setTitle(title);
            adminNotif.setMessage(msg);
            adminNotif.setNotificationType(NotificationType.INCIDENT_ESCALATED.name());
            adminNotif.setSeverity("CRITICAL");
            adminNotif.setDedupKey(dedupKey + "_ADMIN");
            adminNotif.setSentAt(Instant.now());
            adminNotif.setCreatedAt(Instant.now());
            NotificationDTO savedAdmin = notificationDao.save(adminNotif);
            publishWebSocket(savedAdmin);
        } catch (Exception e) {
            log.error("Failed to handle IncidentEscalatedNotificationEvent: {}", e.getMessage(), e);
        }
    }

    private void publishWebSocket(NotificationDTO notif) {
        if (webSocketEventPublisher != null && notif != null) {
            try {
                // If direct user recipient
                if (notif.getRecipientUserId() != null) {
                    webSocketEventPublisher.publishNotificationToUser(notif.getRecipientUserId().toString(), notif);
                } else {
                    webSocketEventPublisher.publishNotification(notif);
                }
                // General broadcast for command staff
                webSocketEventPublisher.publishAlert(new com.smartcity.websocket.dto.AlertEvent(
                        notif.getNotificationType(),
                        notif.getSeverity(),
                        notif.getTitle(),
                        notif.getMessage(),
                        notif.getIncidentId() != null ? notif.getIncidentId().toString() : null,
                        null,
                        null
                ));
            } catch (Exception e) {
                log.warn("Non-fatal WebSocket dispatch warning for notification {}: {}", notif.getId(), e.getMessage());
            }
        }
    }
}
