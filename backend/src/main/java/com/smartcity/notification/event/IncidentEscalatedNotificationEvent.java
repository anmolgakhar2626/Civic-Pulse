package com.smartcity.notification.event;

import java.util.UUID;

public class IncidentEscalatedNotificationEvent {
    private final UUID incidentId;
    private final String incidentNumber;
    private final String escalationLevel;
    private final String reason;
    private final UUID assignedToUserId;
    private final String departmentName;

    public IncidentEscalatedNotificationEvent(
            UUID incidentId,
            String incidentNumber,
            String escalationLevel,
            String reason,
            UUID assignedToUserId,
            String departmentName
    ) {
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.escalationLevel = escalationLevel;
        this.reason = reason;
        this.assignedToUserId = assignedToUserId;
        this.departmentName = departmentName;
    }

    public UUID getIncidentId() { return incidentId; }
    public String getIncidentNumber() { return incidentNumber; }
    public String getEscalationLevel() { return escalationLevel; }
    public String getReason() { return reason; }
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public String getDepartmentName() { return departmentName; }
}
