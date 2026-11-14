package com.smartcity.notification;

import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.incident.Incident;
import com.smartcity.sla.SLARecord;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationDao notificationDao;
    private final NotificationRepository notificationRepository;
    private final List<NotificationSender> senders;
    private final UserRepository userRepository;

    @org.springframework.context.annotation.Lazy
    @Autowired(required = false)
    private com.smartcity.common.AuditService auditService;

    @org.springframework.context.annotation.Lazy
    @Autowired(required = false)
    private com.smartcity.websocket.WebSocketEventPublisher webSocketPublisher;

    @Autowired
    public NotificationServiceImpl(
            @Autowired(required = false) NotificationDao notificationDao,
            NotificationRepository notificationRepository,
            List<NotificationSender> senders,
            UserRepository userRepository
    ) {
        this.notificationDao = notificationDao;
        this.notificationRepository = notificationRepository;
        this.senders = senders != null ? senders : List.of();
        this.userRepository = userRepository;
    }

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            List<NotificationSender> senders,
            UserRepository userRepository
    ) {
        this(null, notificationRepository, senders, userRepository);
    }

    @Override
    public NotificationDTO createNotification(
            User recipient,
            String role,
            String title,
            String message,
            String type,
            NotificationChannel channel
    ) {
        return createNotification(recipient, role, title, message, type, "MEDIUM", null, null, channel);
    }

    @Override
    public NotificationDTO createNotification(
            User recipient,
            String role,
            String title,
            String message,
            String type,
            String severity,
            UUID incidentId,
            String dedupKey,
            NotificationChannel channel
    ) {
        // Prevent duplicate notifications
        if (dedupKey != null && !dedupKey.isBlank()) {
            if (notificationDao != null && notificationDao.existsByDedupKey(dedupKey)) {
                log.debug("Notification with dedupKey {} already exists, skipping.", dedupKey);
                return null;
            }
        }

        Notification notification = new Notification(
                recipient,
                role,
                title,
                message,
                type,
                channel != null ? channel.name() : NotificationChannel.IN_APP.name()
        );
        if (severity != null) {
            notification.setSeverity(severity);
        }
        if (incidentId != null) {
            notification.setIncidentId(incidentId);
        }
        if (dedupKey != null) {
            notification.setDedupKey(dedupKey);
        }

        Notification saved;
        NotificationDTO dto;

        if (notificationDao != null) {
            dto = notificationDao.save(notification);
            saved = notification;
            saved.setId(dto.getId());
        } else {
            saved = notificationRepository.save(notification);
            dto = NotificationDTO.fromEntity(saved);
        }

        if (auditService != null && saved.getId() != null) {
            String details = String.format("{\"title\":\"%s\",\"type\":\"%s\",\"channel\":\"%s\",\"recipientRole\":\"%s\"}",
                    saved.getTitle(), saved.getNotificationType(), saved.getChannel(), saved.getRecipientRole());
            auditService.record("NOTIFICATION", saved.getId().toString(), "NOTIFICATION_DISPATCHED", "SYSTEM/NOTIFICATION_SERVICE", details, "127.0.0.1");
        }

        // Dispatch via appropriate senders (e.g. InApp, Email, SMS)
        if (channel != null) {
            for (NotificationSender sender : senders) {
                if (sender.supports(channel)) {
                    try {
                        sender.send(saved);
                    } catch (Exception e) {
                        log.warn("Failed to dispatch notification {} via {}: {}", saved.getId(), channel, e.getMessage());
                    }
                }
            }
        }

        return dto;
    }

    @Override
    public NotificationDTO createComplaintNotification(
            User recipient,
            String role,
            String title,
            String message,
            String type,
            String severity,
            UUID complaintId,
            String complaintNumber,
            NotificationChannel channel
    ) {
        Notification notification = new Notification(
                recipient,
                role,
                title,
                message,
                type,
                channel != null ? channel.name() : NotificationChannel.IN_APP.name()
        );
        if (severity != null) {
            notification.setSeverity(severity);
        }
        if (complaintId != null) {
            notification.setComplaintId(complaintId);
        }
        if (complaintNumber != null) {
            notification.setComplaintNumber(complaintNumber);
        }

        NotificationDTO dto;
        if (notificationDao != null) {
            dto = notificationDao.save(notification);
        } else {
            Notification saved = notificationRepository.save(notification);
            dto = NotificationDTO.fromEntity(saved);
        }

        // Real-time broadcast
        if (dto != null) {
            try {
                if (recipient != null && recipient.getId() != null) {
                    webSocketPublisher.publishNotificationToUser(recipient.getId().toString(), dto);
                } else {
                    webSocketPublisher.publishNotification(dto);
                }
            } catch (Exception ex) {
                log.debug("WebSocket notification push warning: {}", ex.getMessage());
            }
        }

        return dto;
    }

    @Override
    public void notifyCriticalIncident(Incident incident, String departmentName) {
        String incNum = incident != null ? incident.getIncidentNumber() : "UNKNOWN";
        String incType = incident != null ? (incident.getCategory() != null ? incident.getCategory() : incident.getIncidentType()) : "EMERGENCY";
        String desc = incident != null && incident.getDescription() != null ? incident.getDescription() : "Critical severity event detected.";
        UUID incId = incident != null ? incident.getId() : null;

        String title = "CRITICAL INCIDENT: " + incType + " (" + incNum + ")";
        String msg = "A critical incident has been logged: " + desc + ". Immediate response required.";

        // Command Operator alerts
        createNotification(null, "COMMAND_OPERATOR", title, msg, NotificationType.CRITICAL_ALERT.name(), "CRITICAL", incId, "CRIT_INC_OP_" + incId, NotificationChannel.IN_APP);
        createNotification(null, "COMMAND_OPERATOR", title, msg, NotificationType.CRITICAL_ALERT.name(), "CRITICAL", incId, "CRIT_INC_OP_EMAIL_" + incId, NotificationChannel.EMAIL);
        createNotification(null, "COMMAND_OPERATOR", title, msg, NotificationType.CRITICAL_ALERT.name(), "CRITICAL", incId, "CRIT_INC_OP_SMS_" + incId, NotificationChannel.SMS);

        // Responsible department alert
        String deptMsg = msg + " Assigned Department: " + (departmentName != null ? departmentName : "Unassigned");
        createNotification(null, "DEPARTMENT_OFFICER", title, deptMsg, NotificationType.CRITICAL_ALERT.name(), "CRITICAL", incId, "CRIT_INC_DEPT_" + incId, NotificationChannel.IN_APP);
        createNotification(null, "DEPARTMENT_OFFICER", title, deptMsg, NotificationType.CRITICAL_ALERT.name(), "CRITICAL", incId, "CRIT_INC_DEPT_EMAIL_" + incId, NotificationChannel.EMAIL);

        log.info("Critical incident notification triggered for incident {}", incNum);
    }

    @Override
    public void notifySlaAtRisk(SLARecord slaRecord, String departmentName) {
        String incNum = (slaRecord.getIncident() != null) ? slaRecord.getIncident().getIncidentNumber() : "N/A";
        UUID incId = (slaRecord.getIncident() != null) ? slaRecord.getIncident().getId() : slaRecord.getIncidentId();
        String title = "SLA AT RISK: " + incNum;
        String msg = "Incident " + incNum + " is at risk of breaching SLA. Remaining time: "
                + slaRecord.getRemainingMinutes() + " mins. Target deadline: " + slaRecord.getDeadlineAt()
                + ". Department: " + (departmentName != null ? departmentName : "Assigned Dept");

        createNotification(null, "DEPARTMENT_OFFICER", title, msg, NotificationType.SLA_AT_RISK.name(), "HIGH", incId, "SLA_RISK_DEPT_" + slaRecord.getId(), NotificationChannel.IN_APP);
        createNotification(null, "DEPARTMENT_OFFICER", title, msg, NotificationType.SLA_AT_RISK.name(), "HIGH", incId, "SLA_RISK_DEPT_EMAIL_" + slaRecord.getId(), NotificationChannel.EMAIL);

        log.info("SLA AT_RISK notification dispatched for incident {}", incNum);
    }

    @Override
    public void notifySlaBreached(SLARecord slaRecord, String departmentName) {
        String incNum = (slaRecord.getIncident() != null) ? slaRecord.getIncident().getIncidentNumber() : "N/A";
        UUID incId = (slaRecord.getIncident() != null) ? slaRecord.getIncident().getId() : slaRecord.getIncidentId();
        String title = "SLA BREACHED: " + incNum;
        String msg = "Incident " + incNum + " has breached its resolution SLA target deadline at "
                + slaRecord.getDeadlineAt() + ". Department: " + (departmentName != null ? departmentName : "Assigned Dept");

        createNotification(null, "DEPARTMENT_OFFICER", title, msg, NotificationType.SLA_BREACHED.name(), "CRITICAL", incId, "SLA_BREACH_DEPT_" + slaRecord.getId(), NotificationChannel.IN_APP);
        createNotification(null, "DEPARTMENT_OFFICER", title, msg, NotificationType.SLA_BREACHED.name(), "CRITICAL", incId, "SLA_BREACH_DEPT_EMAIL_" + slaRecord.getId(), NotificationChannel.EMAIL);

        createNotification(null, "COMMAND_OPERATOR", title, msg, NotificationType.SLA_BREACHED.name(), "CRITICAL", incId, "SLA_BREACH_OP_" + slaRecord.getId(), NotificationChannel.IN_APP);
        createNotification(null, "COMMAND_OPERATOR", title, msg, NotificationType.SLA_BREACHED.name(), "CRITICAL", incId, "SLA_BREACH_OP_EMAIL_" + slaRecord.getId(), NotificationChannel.EMAIL);
        createNotification(null, "COMMAND_OPERATOR", title, msg, NotificationType.SLA_BREACHED.name(), "CRITICAL", incId, "SLA_BREACH_OP_SMS_" + slaRecord.getId(), NotificationChannel.SMS);

        log.info("SLA BREACHED notification dispatched for incident {}", incNum);
    }

    @Override
    public void notifyLevel3Escalation(SLARecord slaRecord, String reason) {
        String incNum = (slaRecord.getIncident() != null) ? slaRecord.getIncident().getIncidentNumber() : "N/A";
        UUID incId = (slaRecord.getIncident() != null) ? slaRecord.getIncident().getId() : slaRecord.getIncidentId();
        String title = "LEVEL 3 ESCALATION: " + incNum;
        String msg = "Executive Level 3 Escalation triggered for Incident " + incNum
                + ". Reason: " + (reason != null ? reason : "Critical SLA breach / unresolved status")
                + ". Immediate Command Centre intervention requested.";

        createNotification(null, "ADMIN", title, msg, NotificationType.LEVEL_3_ESCALATION.name(), "CRITICAL", incId, "ESC_L3_ADMIN_" + slaRecord.getId(), NotificationChannel.IN_APP);
        createNotification(null, "ADMIN", title, msg, NotificationType.LEVEL_3_ESCALATION.name(), "CRITICAL", incId, "ESC_L3_ADMIN_EMAIL_" + slaRecord.getId(), NotificationChannel.EMAIL);
        createNotification(null, "ADMIN", title, msg, NotificationType.LEVEL_3_ESCALATION.name(), "CRITICAL", incId, "ESC_L3_ADMIN_SMS_" + slaRecord.getId(), NotificationChannel.SMS);

        createNotification(null, "COMMAND_OPERATOR", title, msg, NotificationType.LEVEL_3_ESCALATION.name(), "CRITICAL", incId, "ESC_L3_OP_" + slaRecord.getId(), NotificationChannel.IN_APP);

        log.info("LEVEL_3 escalation notification dispatched for incident {}", incNum);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDTO getNotificationById(UUID id, UUID userId, List<String> roles) {
        NotificationDTO dto;
        if (notificationDao != null) {
            dto = notificationDao.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        } else {
            Notification n = notificationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
            dto = NotificationDTO.fromEntity(n);
        }
        validateRecipientAccess(dto, userId, roles);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsForUser(
            UUID userId,
            List<String> roles,
            Boolean unreadOnly,
            String type,
            String severity,
            int page,
            int size
    ) {
        return getNotificationsForUser(userId, roles, unreadOnly, type, severity, false, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsForUser(
            UUID userId,
            List<String> roles,
            Boolean unreadOnly,
            String type,
            String severity,
            Boolean directOnly,
            int page,
            int size
    ) {
        List<String> effectiveRoles = roles != null && !roles.isEmpty() ? roles : List.of("CITIZEN");
        int offset = Math.max(0, page) * Math.max(1, size);

        if (notificationDao != null) {
            return notificationDao.findByRecipient(userId, effectiveRoles, unreadOnly, type, severity, directOnly, offset, size);
        }

        // Fallback to JPA repository
        return getNotificationsForUser(userId, roles, unreadOnly);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCountForUser(UUID userId, List<String> roles) {
        List<String> effectiveRoles = roles != null && !roles.isEmpty() ? roles : List.of("CITIZEN");
        if (notificationDao != null) {
            return notificationDao.countUnread(userId, effectiveRoles);
        }

        List<NotificationDTO> list = getNotificationsForUser(userId, roles, true);
        return list.size();
    }

    @Override
    public NotificationDTO markAsRead(UUID id, UUID userId) {
        return markAsRead(id, userId, null);
    }

    @Override
    public NotificationDTO markAsRead(UUID id, UUID userId, List<String> roles) {
        if (notificationDao != null) {
            Optional<NotificationDTO> existing = notificationDao.findById(id);
            if (existing.isEmpty()) {
                throw new ResourceNotFoundException("Notification", "id", id);
            }
            NotificationDTO notif = existing.get();
            validateRecipientAccess(notif, userId, roles);

            notificationDao.markAsRead(id, notif.getRecipientUserId() != null ? userId : null);
            return notificationDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        }

        // Fallback to JPA repository
        return markAsRead(id);
    }

    @Override
    public NotificationDTO markAsUnread(UUID id, UUID userId, List<String> roles) {
        if (notificationDao != null) {
            Optional<NotificationDTO> existing = notificationDao.findById(id);
            if (existing.isEmpty()) {
                throw new ResourceNotFoundException("Notification", "id", id);
            }
            NotificationDTO notif = existing.get();
            validateRecipientAccess(notif, userId, roles);

            notificationDao.markAsUnread(id, notif.getRecipientUserId() != null ? userId : null);
            return notificationDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        }
        return markAsRead(id, userId, roles);
    }

    @Override
    public int markAllAsRead(UUID userId, List<String> roles) {
        List<String> effectiveRoles = roles != null && !roles.isEmpty() ? roles : List.of("CITIZEN");
        if (notificationDao != null) {
            return notificationDao.markAllAsRead(userId, effectiveRoles);
        }
        return markAllAsRead(userId);
    }

    @Override
    public boolean deleteNotification(UUID id, UUID userId) {
        return deleteNotification(id, userId, null);
    }

    @Override
    public boolean deleteNotification(UUID id, UUID userId, List<String> roles) {
        if (notificationDao != null) {
            Optional<NotificationDTO> existing = notificationDao.findById(id);
            if (existing.isEmpty()) {
                throw new ResourceNotFoundException("Notification", "id", id);
            }
            NotificationDTO notif = existing.get();
            validateRecipientAccess(notif, userId, roles);

            return notificationDao.deleteById(id, null);
        }

        // Fallback
        if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id);
            return true;
        }
        throw new ResourceNotFoundException("Notification", "id", id);
    }

    private void validateRecipientAccess(NotificationDTO notif, UUID userId, List<String> roles) {
        List<String> effectiveRoles = roles != null ? roles : List.of();
        boolean isAdmin = effectiveRoles.stream()
                .map(r -> r.toUpperCase().replace("ROLE_", ""))
                .anyMatch(r -> r.equals("ADMIN"));
        if (isAdmin) {
            return;
        }

        // If direct user recipient
        if (notif.getRecipientUserId() != null) {
            if (!notif.getRecipientUserId().equals(userId)) {
                throw new AccessDeniedException("User not authorized for this notification");
            }
            return;
        }

        // If role recipient
        if (notif.getRecipientRole() != null) {
            String roleName = notif.getRecipientRole().toUpperCase().replace("ROLE_", "");
            boolean hasRole = effectiveRoles.stream()
                    .map(r -> r.toUpperCase().replace("ROLE_", ""))
                    .anyMatch(r -> r.equals(roleName));
            if (!hasRole) {
                throw new AccessDeniedException("User role not authorized for this notification");
            }
            return;
        }

        if (userId == null) {
            throw new AccessDeniedException("User not authorized");
        }
    }

    // Backward-compatible methods
    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsForUser(UUID userId, List<String> roles, Boolean unreadOnly) {
        List<String> effectiveRoles = roles != null && !roles.isEmpty() ? roles : List.of("CITIZEN");
        List<Notification> list;

        if (userId == null) {
            list = Boolean.TRUE.equals(unreadOnly)
                    ? notificationRepository.findTop50ByStatusOrderBySentAtDesc("UNREAD")
                    : notificationRepository.findTop50ByOrderBySentAtDesc();
        } else if (Boolean.TRUE.equals(unreadOnly)) {
            list = notificationRepository.findUnreadForUserAndRoles(userId, effectiveRoles);
        } else {
            list = notificationRepository.findForUserAndRoles(userId, effectiveRoles);
        }

        List<NotificationDTO> dtoList = new ArrayList<>();
        for (Notification n : list) {
            dtoList.add(NotificationDTO.fromEntity(n));
        }
        return dtoList;
    }

    @Override
    public NotificationDTO markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));

        notification.setStatus("READ");
        notification.setRead(true);
        notification.setReadAt(Instant.now());
        Notification updated = notificationRepository.save(notification);
        return NotificationDTO.fromEntity(updated);
    }

    @Override
    public int markAllAsRead(UUID userId) {
        Instant now = Instant.now();
        if (userId != null) {
            return notificationRepository.markAllAsReadForUser(userId, now);
        } else {
            return notificationRepository.markAllAsRead(now);
        }
    }
}
