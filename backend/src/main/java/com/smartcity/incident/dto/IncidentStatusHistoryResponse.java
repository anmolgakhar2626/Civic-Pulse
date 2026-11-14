package com.smartcity.incident.dto;

import com.smartcity.incident.IncidentStatusHistory;
import java.time.Instant;
import java.util.UUID;

public class IncidentStatusHistoryResponse {

    private UUID id;
    private UUID incidentId;
    private String previousStatus;
    private String newStatus;
    private String changedBy;
    private Instant changedAt;
    private String remarks;

    public IncidentStatusHistoryResponse() {}

    public static IncidentStatusHistoryResponse fromEntity(IncidentStatusHistory history) {
        if (history == null) {
            return null;
        }

        IncidentStatusHistoryResponse response = new IncidentStatusHistoryResponse();
        response.setId(history.getId());
        if (history.getIncident() != null) {
            response.setIncidentId(history.getIncident().getId());
        }
        response.setPreviousStatus(history.getPreviousStatus());
        response.setNewStatus(history.getNewStatus());
        response.setChangedBy(history.getChangedBy());
        response.setChangedAt(history.getChangedAt());
        response.setRemarks(history.getRemarks());
        return response;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
