package com.smartcity.notification.event;

import java.time.Instant;
import java.util.UUID;

public class SlaWarningNotificationEvent {
    private final UUID slaId;
    private final UUID incidentId;
    private final String incidentNumber;
    private final long remainingMinutes;
    private final Instant deadlineAt;
    private final UUID assignedToUserId;
    private final String departmentName;

    public SlaWarningNotificationEvent(
            UUID slaId,
            UUID incidentId,
            String incidentNumber,
            long remainingMinutes,
            Instant deadlineAt,
            UUID assignedToUserId,
            String departmentName
    ) {
        this.slaId = slaId;
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.remainingMinutes = remainingMinutes;
        this.deadlineAt = deadlineAt;
        this.assignedToUserId = assignedToUserId;
        this.departmentName = departmentName;
    }

    public UUID getSlaId() { return slaId; }
    public UUID getIncidentId() { return incidentId; }
    public String getIncidentNumber() { return incidentNumber; }
    public long getRemainingMinutes() { return remainingMinutes; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public String getDepartmentName() { return departmentName; }
}
