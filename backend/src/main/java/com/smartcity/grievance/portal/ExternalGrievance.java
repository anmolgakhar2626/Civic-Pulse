package com.smartcity.grievance.portal;

import java.time.Instant;
import java.util.UUID;

public class ExternalGrievance {

    private UUID id;
    private UUID complaintId;
    private UUID portalId;
    private String portalCode;
    private String acknowledgementNumber;
    private String idempotencyKey;
    private String externalStatus = "ACKNOWLEDGED";
    private String statusDescription;
    private String officerInCharge;
    private Instant targetResolutionDate;
    private Instant submittedAt = Instant.now();
    private Instant lastSyncedAt = Instant.now();
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public ExternalGrievance() {}

    public ExternalGrievance(UUID complaintId, UUID portalId, String portalCode,
                             String acknowledgementNumber, String idempotencyKey) {
        this.id = UUID.randomUUID();
        this.complaintId = complaintId;
        this.portalId = portalId;
        this.portalCode = portalCode;
        this.acknowledgementNumber = acknowledgementNumber;
        this.idempotencyKey = idempotencyKey;
        this.externalStatus = "ACKNOWLEDGED";
        this.submittedAt = Instant.now();
        this.lastSyncedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getComplaintId() { return complaintId; }
    public void setComplaintId(UUID complaintId) { this.complaintId = complaintId; }

    public UUID getPortalId() { return portalId; }
    public void setPortalId(UUID portalId) { this.portalId = portalId; }

    public String getPortalCode() { return portalCode; }
    public void setPortalCode(String portalCode) { this.portalCode = portalCode; }

    public String getAcknowledgementNumber() { return acknowledgementNumber; }
    public void setAcknowledgementNumber(String acknowledgementNumber) { this.acknowledgementNumber = acknowledgementNumber; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }

    public String getStatusDescription() { return statusDescription; }
    public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }

    public String getOfficerInCharge() { return officerInCharge; }
    public void setOfficerInCharge(String officerInCharge) { this.officerInCharge = officerInCharge; }

    public Instant getTargetResolutionDate() { return targetResolutionDate; }
    public void setTargetResolutionDate(Instant targetResolutionDate) { this.targetResolutionDate = targetResolutionDate; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
