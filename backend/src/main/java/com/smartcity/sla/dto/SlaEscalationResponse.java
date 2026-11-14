package com.smartcity.sla.dto;

import java.time.Instant;
import java.util.UUID;

public class SlaEscalationResponse {

    private UUID id;
    private UUID incidentId;
    private String incidentNumber;
    private String incidentTitle;
    private UUID slaRecordId;
    private String previousEscalationLevel;
    private String newEscalationLevel;
    private String previousStatus;
    private String newStatus;
    private String reason;
    private Instant escalatedAt;

    public SlaEscalationResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }

    public String getIncidentTitle() { return incidentTitle; }
    public void setIncidentTitle(String incidentTitle) { this.incidentTitle = incidentTitle; }

    public UUID getSlaRecordId() { return slaRecordId; }
    public void setSlaRecordId(UUID slaRecordId) { this.slaRecordId = slaRecordId; }

    public String getPreviousEscalationLevel() { return previousEscalationLevel; }
    public void setPreviousEscalationLevel(String previousEscalationLevel) { this.previousEscalationLevel = previousEscalationLevel; }

    public String getNewEscalationLevel() { return newEscalationLevel; }
    public void setNewEscalationLevel(String newEscalationLevel) { this.newEscalationLevel = newEscalationLevel; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(Instant escalatedAt) { this.escalatedAt = escalatedAt; }
}

