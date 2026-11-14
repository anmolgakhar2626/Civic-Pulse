package com.smartcity.assignment.dto;

import com.smartcity.assignment.ResponseAction;
import com.smartcity.assignment.ResponseActionStatus;
import com.smartcity.assignment.ResponseActionType;

import java.time.Instant;
import java.util.UUID;

public class ResponseActionResponse {

    private UUID id;
    private UUID incidentId;
    private String incidentNumber;
    private ResponseActionType actionType;
    private String description;
    private ResponseActionStatus status;
    private String createdBy;
    private Instant createdAt;
    private Instant completedAt;

    public ResponseActionResponse() {}

    public static ResponseActionResponse fromEntity(ResponseAction entity) {
        if (entity == null) {
            return null;
        }

        ResponseActionResponse dto = new ResponseActionResponse();
        dto.setId(entity.getId());
        if (entity.getIncident() != null) {
            dto.setIncidentId(entity.getIncident().getId());
            dto.setIncidentNumber(entity.getIncident().getIncidentNumber());
        }
        dto.setActionType(entity.getActionType());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public String getIncidentNumber() { return incidentNumber; }
    public void setIncidentNumber(String incidentNumber) { this.incidentNumber = incidentNumber; }

    public ResponseActionType getActionType() { return actionType; }
    public void setActionType(ResponseActionType actionType) { this.actionType = actionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ResponseActionStatus getStatus() { return status; }
    public void setStatus(ResponseActionStatus status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
