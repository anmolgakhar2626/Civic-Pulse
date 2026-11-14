package com.smartcity.notification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Municipal Notification DTO")
public class NotificationDTO {

    @Schema(description = "Notification primary ID", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
    private UUID id;

    @Schema(description = "Recipient user ID", example = "a5866919-cd90-47ac-806a-09a7d0bea677")
    private UUID recipientUserId;

    @Schema(description = "Recipient username", example = "operator")
    private String recipientUsername;

    @Schema(description = "Recipient role", example = "COMMAND_OPERATOR")
    private String recipientRole;

    @Schema(description = "Notification title", example = "CRITICAL INCIDENT: Flood")
    private String title;

    @Schema(description = "Notification message", example = "Severe Flash Flood reported in Sector 17 Underpass.")
    private String message;

    @Schema(description = "Notification event type", example = "CRITICAL_ALERT")
    private String notificationType;

    @Schema(description = "Related incident ID", example = "c8f2b7a9-9521-4f18-bb98-933c09b7c844")
    private UUID incidentId;

    @Schema(description = "Related complaint ID")
    private UUID complaintId;

    @Schema(description = "Related complaint record number", example = "CP-2026-000001")
    private String complaintNumber;

    @Schema(description = "Severity tier", example = "CRITICAL")
    private String severity = "MEDIUM";

    @Schema(description = "Notification delivery channel", example = "IN_APP")
    private String channel = "IN_APP";

    @Schema(description = "Status string", example = "UNREAD")
    private String status = "UNREAD";

    @Schema(description = "Read boolean flag", example = "false")
    private boolean isRead = false;

    @Schema(description = "Deduplication key", example = "INC_CREATED_c8f2b7a9-9521-4f18-bb98-933c09b7c844")
    private String dedupKey;

    @Schema(description = "Timestamp when notification was dispatched")
    private Instant sentAt;

    @Schema(description = "Timestamp when notification was marked read")
    private Instant readAt;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    public NotificationDTO() {}

    public static NotificationDTO fromEntity(Notification entity) {
        if (entity == null) return null;
        NotificationDTO dto = new NotificationDTO();
        dto.setId(entity.getId());
        if (entity.getRecipientUser() != null) {
            dto.setRecipientUserId(entity.getRecipientUser().getId());
            dto.setRecipientUsername(entity.getRecipientUser().getUsername());
        }
        dto.setRecipientRole(entity.getRecipientRole());
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setNotificationType(entity.getNotificationType());
        dto.setIncidentId(entity.getIncidentId());
        dto.setComplaintId(entity.getComplaintId());
        dto.setComplaintNumber(entity.getComplaintNumber());
        dto.setSeverity(entity.getSeverity() != null ? entity.getSeverity() : "MEDIUM");
        dto.setChannel(entity.getChannel());
        dto.setStatus(entity.getStatus());
        dto.setRead(entity.isRead() || "READ".equalsIgnoreCase(entity.getStatus()));
        dto.setDedupKey(entity.getDedupKey());
        dto.setSentAt(entity.getSentAt());
        dto.setReadAt(entity.getReadAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(UUID recipientUserId) { this.recipientUserId = recipientUserId; }

    public String getRecipientUsername() { return recipientUsername; }
    public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }

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
    public void setSeverity(String severity) { this.severity = severity; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
        this.isRead = "READ".equalsIgnoreCase(status);
    }

    @com.fasterxml.jackson.annotation.JsonProperty("read")
    public boolean isRead() { return isRead; }

    @com.fasterxml.jackson.annotation.JsonProperty("isRead")
    public boolean getIsRead() { return isRead; }

    public void setRead(boolean read) {
        this.isRead = read;
        this.status = read ? "READ" : "UNREAD";
    }

    public void setIsRead(boolean isRead) {
        setRead(isRead);
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
