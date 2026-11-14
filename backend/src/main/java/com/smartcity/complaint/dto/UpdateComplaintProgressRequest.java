package com.smartcity.complaint.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateComplaintProgressRequest {
    @NotBlank(message = "Status is required")
    private String status;

    private String remarks;
    private String workNotes;
    private String progressNotes;
    private String resolutionNotes;

    public UpdateComplaintProgressRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getWorkNotes() {
        if (workNotes != null && !workNotes.isBlank()) return workNotes;
        if (progressNotes != null && !progressNotes.isBlank()) return progressNotes;
        return remarks;
    }
    public void setWorkNotes(String workNotes) {
        this.workNotes = workNotes;
        if (this.remarks == null) this.remarks = workNotes;
        if (this.progressNotes == null) this.progressNotes = workNotes;
    }

    public String getProgressNotes() {
        if (progressNotes != null && !progressNotes.isBlank()) return progressNotes;
        if (workNotes != null && !workNotes.isBlank()) return workNotes;
        return remarks;
    }
    public void setProgressNotes(String progressNotes) {
        this.progressNotes = progressNotes;
        if (this.remarks == null) this.remarks = progressNotes;
        if (this.workNotes == null) this.workNotes = progressNotes;
    }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}
