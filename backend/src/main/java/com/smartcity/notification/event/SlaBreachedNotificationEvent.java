package com.smartcity.notification.event;

import java.time.Instant;
import java.util.UUID;

public class SlaBreachedNotificationEvent {
    private final UUID slaId;
    private final UUID incidentId;
    private final String incidentNumber;
    private final Instant breachedAt;
    private final UUID assignedToUserId;
    private final String departmentName;

    public SlaBreachedNotificationEvent(
            UUID slaId,
            UUID incidentId,
            String incidentNumber,
            Instant breachedAt,
            UUID assignedToUserId,
            String departmentName
    ) {
        this.slaId = slaId;
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.breachedAt = breachedAt;
        this.assignedToUserId = assignedToUserId;
        this.departmentName = departmentName;
    }

    public UUID getSlaId() { return slaId; }
    public UUID getIncidentId() { return incidentId; }
    public String getIncidentNumber() { return incidentNumber; }
    public Instant getBreachedAt() { return breachedAt; }
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public String getDepartmentName() { return departmentName; }
}
