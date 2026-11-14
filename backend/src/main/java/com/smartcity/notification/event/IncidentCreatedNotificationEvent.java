package com.smartcity.notification.event;

import java.util.UUID;

public class IncidentCreatedNotificationEvent {
    private final UUID incidentId;
    private final String incidentNumber;
    private final String title;
    private final String category;
    private final String severity;
    private final UUID reportedByUserId;
    private final UUID assignedToUserId;
    private final String departmentName;

    public IncidentCreatedNotificationEvent(
            UUID incidentId,
            String incidentNumber,
            String title,
            String category,
            String severity,
            UUID reportedByUserId,
            UUID assignedToUserId,
            String departmentName
    ) {
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.title = title;
        this.category = category;
        this.severity = severity != null ? severity.toUpperCase() : "MEDIUM";
        this.reportedByUserId = reportedByUserId;
        this.assignedToUserId = assignedToUserId;
        this.departmentName = departmentName;
    }

    public UUID getIncidentId() { return incidentId; }
    public String getIncidentNumber() { return incidentNumber; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getSeverity() { return severity; }
    public UUID getReportedByUserId() { return reportedByUserId; }
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public String getDepartmentName() { return departmentName; }
}
