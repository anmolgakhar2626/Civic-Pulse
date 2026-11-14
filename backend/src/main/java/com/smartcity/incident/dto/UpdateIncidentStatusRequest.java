package com.smartcity.incident.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.smartcity.incident.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateIncidentStatusRequest {

    @NotNull(message = "Status is required")
    private IncidentStatus status;

    @Size(max = 100, message = "ChangedBy cannot exceed 100 characters")
    private String changedBy;

    @JsonAlias("notes")
    private String remarks;

    public UpdateIncidentStatusRequest() {}

    public UpdateIncidentStatusRequest(IncidentStatus status, String changedBy, String remarks) {
        this.status = status;
        this.changedBy = changedBy;
        this.remarks = remarks;
    }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getNotes() { return remarks; }
    public void setNotes(String notes) { this.remarks = notes; }
}
