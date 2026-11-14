package com.smartcity.websocket.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Reusable Alert Event model for critical occurrences across the city.
 * Published to /topic/alerts.
 */
public class AlertEvent {

    private String alertId;
    private String type; // CRITICAL_INCIDENT, HIGH_PRIORITY_INCIDENT, SENSOR_THRESHOLD, SLA_AT_RISK, SLA_BREACHED, ESCALATION
    private String severity; // INFO, LOW, MEDIUM, HIGH, CRITICAL, EXTREME
    private String title;
    private String message;
    private String incidentId;
    private Double latitude;
    private Double longitude;
    private Instant timestamp;

    public AlertEvent() {
        this.alertId = "ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.timestamp = Instant.now();
    }

    public AlertEvent(String type, String severity, String title, String message,
                      String incidentId, Double latitude, Double longitude) {
        this.alertId = "ALT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.incidentId = incidentId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = Instant.now();
    }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp != null ? timestamp : Instant.now(); }
}
