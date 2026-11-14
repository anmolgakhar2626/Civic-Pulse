package com.smartcity.complaint;

import java.time.Instant;
import java.util.UUID;

public class ComplaintVerificationHistory {

    private UUID id;
    private UUID complaintId;
    private UUID citizenId;
    private String previousStatus;
    private String newStatus;
    private String decisionReason;
    private UUID verifiedBy;
    private String verifiedByName;
    private Instant createdAt;

    public ComplaintVerificationHistory() {}

    public ComplaintVerificationHistory(UUID complaintId, UUID citizenId, String previousStatus,
                                      String newStatus, String decisionReason, UUID verifiedBy,
                                      String verifiedByName) {
        this.id = UUID.randomUUID();
        this.complaintId = complaintId;
        this.citizenId = citizenId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.decisionReason = decisionReason;
        this.verifiedBy = verifiedBy;
        this.verifiedByName = verifiedByName;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getComplaintId() { return complaintId; }
    public void setComplaintId(UUID complaintId) { this.complaintId = complaintId; }

    public UUID getCitizenId() { return citizenId; }
    public void setCitizenId(UUID citizenId) { this.citizenId = citizenId; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }

    public UUID getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(UUID verifiedBy) { this.verifiedBy = verifiedBy; }

    public String getVerifiedByName() { return verifiedByName; }
    public void setVerifiedByName(String verifiedByName) { this.verifiedByName = verifiedByName; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
