package com.smartcity.notification.event;

import java.util.UUID;

public class IncidentStatusChangedNotificationEvent {
    private final UUID incidentId;
    private final String incidentNumber;
    private final String title;
    private final String oldStatus;
    private final String newStatus;
    private final String changedByUsername;
    private final UUID reportedByUserId;
    private final UUID assignedToUserId;

    public IncidentStatusChangedNotificationEvent(
            UUID incidentId,
            String incidentNumber,
            String title,
            String oldStatus,
            String newStatus,
            String changedByUsername,
            UUID reportedByUserId,
            UUID assignedToUserId
    ) {
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.title = title;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedByUsername = changedByUsername;
        this.reportedByUserId = reportedByUserId;
        this.assignedToUserId = assignedToUserId;
    }

    public UUID getIncidentId() { return incidentId; }
    public String getIncidentNumber() { return incidentNumber; }
    public String getTitle() { return title; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getChangedByUsername() { return changedByUsername; }
    public UUID getReportedByUserId() { return reportedByUserId; }
    public UUID getAssignedToUserId() { return assignedToUserId; }
}
