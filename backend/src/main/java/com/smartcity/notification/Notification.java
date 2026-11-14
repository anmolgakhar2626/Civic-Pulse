package com.smartcity.notification;

import com.smartcity.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = true)
    private User recipientUser;

    @Column(name = "recipient_role", length = 100)
    private String recipientRole;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "complaint_id")
    private UUID complaintId;

    @Column(name = "complaint_number", length = 50)
    private String complaintNumber;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(nullable = false, length = 50)
    private String channel = "IN_APP"; // IN_APP, SMS, EMAIL, WEBSOCKET

    @Column(nullable = false, length = 50)
    private String status = "UNREAD"; // UNREAD, READ, DISMISSED

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "dedup_key", length = 255)
    private String dedupKey;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Notification() {}

    public Notification(User recipientUser, String title, String message, String notificationType, String channel) {
        this(recipientUser, null, title, message, notificationType, channel);
    }

    public Notification(User recipientUser, String recipientRole, String title, String message, String notificationType, String channel) {
        this.recipientUser = recipientUser;
        this.recipientRole = recipientRole;
        this.title = title;
        this.message = message;
        this.notificationType = notificationType;
        this.channel = channel != null ? channel : "IN_APP";
        this.status = "UNREAD";
        this.isRead = false;
        this.sentAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getRecipientUser() { return recipientUser; }
    public void setRecipientUser(User recipientUser) { this.recipientUser = recipientUser; }

    public String getRecipientRole() { return recipientRole; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public UUID getComplaintId() { return complaintId; }
    public void setComplaintId(UUID complaintId) { this.complaintId = complaintId; }

    public String getComplaintNumber() { return complaintNumber; }
    public void setComplaintNumber(String complaintNumber) { this.complaintNumber = complaintNumber; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity != null ? severity.toUpperCase() : "MEDIUM"; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.isRead = "READ".equalsIgnoreCase(status);
    }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) {
        this.isRead = read;
        this.status = read ? "READ" : "UNREAD";
        if (read && this.readAt == null) {
            this.readAt = Instant.now();
        }
    }

    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
