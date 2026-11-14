package com.smartcity.websocket.dto;

import java.time.Instant;

/**
 * Frontend-safe real-time Incident lifecycle event model.
 * Published to /topic/incidents.
 */
public class IncidentEvent {

    private String incidentId;
    private String incidentNumber;
    private String incidentType;
    private String title;
    private String priorityTier;
    private String priorityLabel;
    private String status;
    private Double latitude;
    private Double longitude;
    private String assignedDepartment;
    private Instant timestamp;
    private String eventType; // CREATED, PRIORITIZED, ASSIGNED, STATUS_CHANGED, ESCALATED, RESOLVED, CLOSED

    public IncidentEvent() {
        this.timestamp = Instant.now();
    }

    public IncidentEvent(String incidentId, String incidentNumber, String incidentType, String title,
                         String priorityTier, String priorityLabel, String status, Double latitude, Double longitude,
                         String assignedDepartment, Instant timestamp, String eventType) {
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.incidentType = incidentType;
        this.title = title;
        this.priorityTier = priorityTier;
        this.priorityLabel = priorityLabel;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.assignedDepartment = assignedDepartment;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.eventType = eventType;
    }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }

    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPriorityTier() { return priorityTier; }
    public void setPriorityTier(String priorityTier) { this.priorityTier = priorityTier; }

    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAssignedDepartment() { return assignedDepartment; }
    public void setAssignedDepartment(String assignedDepartment) { this.assignedDepartment = assignedDepartment; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp != null ? timestamp : Instant.now(); }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
}
