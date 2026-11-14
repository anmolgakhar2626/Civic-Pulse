package com.smartcity.websocket.dto;

import java.time.Instant;

/**
 * Real-time SLA lifecycle and risk event model.
 * Published to /topic/sla.
 */
public class SLAEvent {

    private String incidentId;
    private String slaId;
    private String priorityTier;
    private Instant deadlineAt;
    private Long remainingMinutes;
    private String slaStatus; // ON_TRACK, AT_RISK, BREACHED, RESOLVED, ESCALATED
    private String escalationLevel;
    private Instant timestamp;

    public SLAEvent() {
        this.timestamp = Instant.now();
    }

    public SLAEvent(String incidentId, String slaId, String priorityTier, Instant deadlineAt,
                    Long remainingMinutes, String slaStatus, String escalationLevel, Instant timestamp) {
        this.incidentId = incidentId;
        this.slaId = slaId;
        this.priorityTier = priorityTier;
        this.deadlineAt = deadlineAt;
        this.remainingMinutes = remainingMinutes;
        this.slaStatus = slaStatus;
        this.escalationLevel = escalationLevel;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getSlaId() { return slaId; }
    public void setSlaId(String slaId) { this.slaId = slaId; }

    public String getPriorityTier() { return priorityTier; }
    public void setPriorityTier(String priorityTier) { this.priorityTier = priorityTier; }

    public Instant getDeadlineAt() { return deadlineAt; }
    public void setDeadlineAt(Instant deadlineAt) { this.deadlineAt = deadlineAt; }

    public Long getRemainingMinutes() { return remainingMinutes; }
    public void setRemainingMinutes(Long remainingMinutes) { this.remainingMinutes = remainingMinutes; }

    public String getSlaStatus() { return slaStatus; }
    public void setSlaStatus(String slaStatus) { this.slaStatus = slaStatus; }

    public String getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(String escalationLevel) { this.escalationLevel = escalationLevel; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp != null ? timestamp : Instant.now(); }
}
