package com.smartcity.complaint.dto;

import com.smartcity.complaint.ComplaintStatusHistory;
import java.time.Instant;
import java.util.UUID;

public class ComplaintStatusHistoryDTO {
    private UUID id;
    private UUID complaintId;
    private String previousStatus;
    private String newStatus;
    private UUID changedBy;
    private String changedByName;
    private String changedByRole;
    private String changeReason;
    private String remarks;
    private Instant createdAt;

    public ComplaintStatusHistoryDTO() {}

    public ComplaintStatusHistoryDTO(ComplaintStatusHistory h) {
        if (h != null) {
            this.id = h.getId();
            this.complaintId = h.getComplaintId();
            this.previousStatus = h.getPreviousStatus();
            this.newStatus = h.getNewStatus();
            this.changedBy = h.getChangedBy();
            this.changedByName = h.getChangedByName();
            this.changedByRole = h.getChangedByRole();
            this.changeReason = h.getChangeReason();
            this.remarks = h.getRemarks();
            this.createdAt = h.getCreatedAt();
        }
    }

    public static ComplaintStatusHistoryDTO fromEntity(ComplaintStatusHistory h) {
        return h != null ? new ComplaintStatusHistoryDTO(h) : null;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getComplaintId() { return complaintId; }
    public void setComplaintId(UUID complaintId) { this.complaintId = complaintId; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public UUID getChangedBy() { return changedBy; }
    public void setChangedBy(UUID changedBy) { this.changedBy = changedBy; }

    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }

    public String getChangedByRole() { return changedByRole; }
    public void setChangedByRole(String changedByRole) { this.changedByRole = changedByRole; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
