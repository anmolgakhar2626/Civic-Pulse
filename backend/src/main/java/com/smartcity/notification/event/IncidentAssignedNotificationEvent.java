package com.smartcity.notification.event;

import java.util.UUID;

public class IncidentAssignedNotificationEvent {
    private final UUID incidentId;
    private final String incidentNumber;
    private final String title;
    private final String severity;
    private final UUID assignedToUserId;
    private final String assignedByUsername;

    public IncidentAssignedNotificationEvent(
            UUID incidentId,
            String incidentNumber,
            String title,
            String severity,
            UUID assignedToUserId,
            String assignedByUsername
    ) {
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.title = title;
        this.severity = severity != null ? severity.toUpperCase() : "MEDIUM";
        this.assignedToUserId = assignedToUserId;
        this.assignedByUsername = assignedByUsername;
    }

    public UUID getIncidentId() { return incidentId; }
    public String getIncidentNumber() { return incidentNumber; }
    public String getTitle() { return title; }
    public String getSeverity() { return severity; }
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public String getAssignedByUsername() { return assignedByUsername; }
}
